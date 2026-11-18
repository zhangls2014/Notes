package me.zhangls.download

import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.coroutines.coroutineContext

internal const val FRAGMENT_BYTES = 256L * 1024
private val sha256Pattern = Regex("[a-f0-9]{64}")
private val targetLockGuard = Mutex()
private val targetLocks = mutableMapOf<Path, TargetLock>()

private class TargetLock(val mutex: Mutex = Mutex(), var users: Int = 0)

internal suspend fun <T> withDownloadTarget(target: Path, action: suspend () -> T): T {
  val entry = targetLockGuard.withLock {
    targetLocks.getOrPut(target, ::TargetLock).also { it.users++ }
  }
  try {
    return entry.mutex.withLock { action() }
  } finally {
    withContext(NonCancellable) {
      targetLockGuard.withLock {
        if (--entry.users == 0) targetLocks.remove(target)
      }
    }
  }
}

/**
 * Downloads a versioned resource into a caller-owned absolute file path.
 * The client must use the caller's redirect and authentication policy.
 * Only a complete file matching [download]'s size and SHA-256 is published.
 */
class ResumableDownloader(private val client: HttpClient) {
  /** Reports received bytes on the I/O dispatcher. Retries can return to an earlier checkpoint. */
  suspend fun download(
    url: String,
    destination: String,
    expectedSize: Long,
    sha256: String,
    requestTimeoutMillis: Long? = null,
    onProgress: (Long) -> Unit = {},
  ) = withContext(Dispatchers.IO) {
    val target = Path(destination)
    require(target.isAbsolute && expectedSize > 0 && sha256Pattern.matches(sha256))
    val parent = requireNotNull(target.parent)
    withDownloadTarget(target) {
      if (checkedFile(target, expectedSize, sha256)) return@withDownloadTarget
      SystemFileSystem.createDirectories(parent)
      val partial = Path(parent, checkpointName(target.name, expectedSize, sha256))
      var reported = -1L
      val report: (Long) -> Unit = { bytes ->
        if (bytes != reported) {
          reported = bytes
          onProgress(bytes)
        }
      }
      var offset = resumeOffset(partial, expectedSize)
      report(offset)
      while (offset < expectedSize) {
        ensureActive()
        offset = retryTransport {
          val start = resumeOffset(partial, expectedSize)
          report(start)
          if (start == expectedSize) start else {
            val length = minOf(FRAGMENT_BYTES, expectedSize - start)
            request(url, partial, start, length, expectedSize, requestTimeoutMillis, report)
          }
        }
        report(offset)
      }
      if (!checkedFile(partial, expectedSize, sha256)) {
        truncateDownload(partial.toString(), 0)
        throw InvalidDownload("Resource size or SHA-256 does not match")
      }
      ensureActive()
      SystemFileSystem.atomicMove(partial, target)
    }
  }

  private suspend fun request(
    url: String,
    partial: Path,
    start: Long,
    length: Long,
    size: Long,
    timeoutMillis: Long?,
    onProgress: (Long) -> Unit,
  ): Long = client.prepareGet(url) {
    expectSuccess = false
    header(HttpHeaders.Range, "bytes=$start-${start + length - 1}")
    header(HttpHeaders.AcceptEncoding, "identity")
    if (timeoutMillis != null) timeout { requestTimeoutMillis = timeoutMillis }
  }.execute { response ->
    val encoding = response.headers[HttpHeaders.ContentEncoding]
    when (response.status) {
      HttpStatusCode.OK -> {
        requireIdentityEncoding(encoding)
        val body = response.bodyAsChannel()
        if (start > 0) onProgress(0)
        val hasExtraBytes = SystemFileSystem.sink(partial).buffered().use { sink ->
          body.receive(size, sink, onProgress)
          body.readBuffer(1).size != 0L
        }
        if (hasExtraBytes) {
          truncateDownload(partial.toString(), 0)
          throw InvalidDownload("Full response exceeds expected size")
        }
        size
      }

      HttpStatusCode.PartialContent -> {
        requireIdentityEncoding(encoding)
        val expectedRange = "bytes $start-${start + length - 1}/$size"
        if (!expectedRange.equals(response.headers[HttpHeaders.ContentRange], ignoreCase = true)) {
          throw InvalidDownload("Unexpected Content-Range")
        }
        val body = response.bodyAsChannel()
        val fragment = Buffer()
        body.receive(length, fragment) { onProgress(start + it) }
        if (body.readBuffer(1).size != 0L) throw InvalidDownload("Fragment exceeds the requested range")
        // Only completed response fragments are appended. If a previous write was interrupted, replay from its start.
        if (resumeOffset(partial, size) != start) throw InvalidDownload("Download checkpoint changed")
        SystemFileSystem.sink(partial, append = true).buffered().use { it.write(fragment, length) }
        start + length
      }

      else -> {
        val status = response.status.value
        if (status == 408 || status == 429 || status in 500..599) throw IOException("HTTP $status")
        throw InvalidDownload("HTTP $status")
      }
    }
  }
}

private suspend fun ByteReadChannel.receive(length: Long, sink: Sink, onProgress: (Long) -> Unit) {
  val buffer = ByteArray(minOf(length, 64 * 1024L).toInt())
  var received = 0L
  while (received < length) {
    coroutineContext.ensureActive()
    val count = readAvailable(buffer, 0, minOf(buffer.size.toLong(), length - received).toInt())
    if (count < 0) throw IOException("Response ended early")
    sink.write(buffer, 0, count)
    received += count
    if (received < length) onProgress(received)
  }
}

private fun requireIdentityEncoding(encoding: String?) {
  if (encoding != null && !encoding.equals("identity", ignoreCase = true)) {
    throw InvalidDownload("Unexpected content encoding")
  }
}

private fun resumeOffset(partial: Path, expectedSize: Long): Long {
  val size = SystemFileSystem.metadataOrNull(partial)?.size ?: return 0
  val alignedSize = checkpointOffset(size, expectedSize)
  if (size != alignedSize) truncateDownload(partial.toString(), alignedSize)
  return alignedSize
}

private fun checkedFile(path: Path, size: Long, sha256: String): Boolean {
  val metadata = SystemFileSystem.metadataOrNull(path) ?: return false
  return metadata.isRegularFile && metadata.size == size && downloadSha256(path.toString()) == sha256
}

private suspend fun <T> retryTransport(action: suspend () -> T): T {
  repeat(2) { attempt ->
    try {
      return action()
    } catch (failure: IOException) {
      if (failure is InvalidDownload) throw failure
      delay(250L * (attempt + 1))
    }
  }
  return action()
}

private class InvalidDownload(message: String) : IOException(message)

internal expect fun truncateDownload(path: String, size: Long)
internal expect fun downloadSha256(path: String): String
