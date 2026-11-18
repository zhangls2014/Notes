@file:OptIn(io.ktor.utils.io.InternalAPI::class)

package me.zhangls.download

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

private const val URL = "https://download.example.test/resource"
private const val CHUNK = 256 * 1024
private val payload = ByteArray(CHUNK + 5) { (it % 251).toByte() }
private const val SHA256 = "f5b34cb5a0115608cc99636dead56147040c85b3e96712ff2e0363afe7d3744c"

class ResumableDownloaderTest {
  @Test
  fun countsRecoverableCheckpointsWithoutCountingOverlappingOrIncompleteParts() {
    val base = checkpointName("asset.wasm", 4L * CHUNK + 5, "a".repeat(64))
    val files = mapOf(
      base to CHUNK.toLong() + 3,
      "$base.0" to CHUNK.toLong(),
      "$base.${2 * CHUNK}" to CHUNK.toLong(),
      "$base.${3 * CHUNK}" to CHUNK.toLong() - 1,
      "$base.${4 * CHUNK}" to 5L,
    )
    assertEquals(
      listOf(DownloadCheckpoint("asset.wasm", 4L * CHUNK + 5, 2L * CHUNK + 5)),
      downloadCheckpoints(files),
    )
  }

  @Test
  fun reportsBytesBeforeTheFirstFragmentFinishesAndResetsOnRetry() = runTest {
    withTarget { target ->
      val response = ByteChannel(autoFlush = true)
      val server = TestServer().apply { firstRangeBody = response }
      val firstProgress = CompletableDeferred<Long>()
      val progress = mutableListOf<Long>()
      HttpClient(server).use { client ->
        val download = launch {
          ResumableDownloader(client).download(URL, target.toString(), payload.size.toLong(), SHA256) { bytes ->
            progress += bytes
            if (bytes > 0) firstProgress.complete(bytes)
          }
        }
        try {
          response.writeFully(payload, 0, 4096)
          val shown = withContext(Dispatchers.Default) { withTimeout(5_000) { firstProgress.await() } }
          assertEquals(4096L, shown)
          assertEquals(emptyList(), SystemFileSystem.list(checkNotNull(target.parent)))
        } finally {
          // Close an incomplete response so the first fragment must be fetched again.
          response.flushAndClose()
        }
        download.join()
        assertEquals(listOf(0L, 4096L, 0L), progress.take(3))
        assertEquals(listOf("bytes=0-262143", "bytes=0-262143", "bytes=262144-262148"), server.ranges)
        assertContentEquals(payload, SystemFileSystem.source(target).buffered().use { it.readByteArray() })
      }
    }
  }

  @Test
  fun resumesFromTheLastCompleteFragmentAcrossInstances() = runTest {
    withTarget { target ->
      val server = TestServer()
      HttpClient(server).use { client ->
        server.interrupted = true
        assertFailsWith<IOException> { ResumableDownloader(client).download(URL, target.toString(), payload.size.toLong(), SHA256) }
        assertFalse(SystemFileSystem.exists(target))
        assertEquals(listOf("bytes=0-262143", "bytes=262144-262148"), server.ranges.distinct())

        val checkpoint = SystemFileSystem.list(checkNotNull(target.parent)).single()
        // Simulate the process stopping during the next write.
        SystemFileSystem.sink(checkpoint, append = true).buffered().use { it.write(byteArrayOf(8, 9)) }
        server.ranges.clear()
        server.interrupted = false
        val progress = mutableListOf<Long>()
        ResumableDownloader(client).download(URL, target.toString(), payload.size.toLong(), SHA256) { progress += it }

        assertEquals(listOf("bytes=262144-262148"), server.ranges)
        assertEquals(listOf(CHUNK.toLong(), payload.size.toLong()), progress)
        assertContentEquals(payload, SystemFileSystem.source(target).buffered().use { it.readByteArray() })
      }
    }
  }

  @Test
  fun fullResponseSafelyReplacesPartialDataAndPublishesOnlyVerifiedBytes() = runTest {
    withTarget { target ->
      val server = TestServer()
      HttpClient(server).use { client ->
        val downloader = ResumableDownloader(client)
        server.interrupted = true
        assertFailsWith<IOException> { downloader.download(URL, target.toString(), payload.size.toLong(), SHA256) }

        server.interrupted = false
        server.ignoreRange = true
        server.corrupt = true
        assertFailsWith<IOException> { downloader.download(URL, target.toString(), payload.size.toLong(), SHA256) }
        assertFalse(SystemFileSystem.exists(target))

        server.corrupt = false
        downloader.download(URL, target.toString(), payload.size.toLong(), SHA256)
        assertContentEquals(payload, SystemFileSystem.source(target).buffered().use { it.readByteArray() })
      }
    }
  }

  @Test
  fun rejectsAResponseForTheWrongByteRange() = runTest {
    withTarget { target ->
      val server = TestServer().apply { wrongRange = true }
      HttpClient(server).use { client ->
        assertFailsWith<IOException> {
          ResumableDownloader(client).download(URL, target.toString(), payload.size.toLong(), SHA256)
        }
        assertEquals(listOf("bytes=0-262143"), server.ranges)
        assertFalse(SystemFileSystem.exists(target))
      }
    }
  }

  private suspend fun withTarget(test: suspend (Path) -> Unit) {
    val directory = Path(SystemTemporaryDirectory, "notes-download-test-${Random.nextLong()}")
    SystemFileSystem.createDirectories(directory)
    try {
      test(Path(directory, "asset"))
    } finally {
      SystemFileSystem.list(directory).forEach { SystemFileSystem.delete(it, mustExist = false) }
      SystemFileSystem.delete(directory)
    }
  }
}

private class TestServer : HttpClientEngineBase("DownloadTest") {
  override val config = HttpClientEngineConfig()
  val ranges = mutableListOf<String>()
  var interrupted = false
  var ignoreRange = false
  var corrupt = false
  var wrongRange = false
  var firstRangeBody: ByteReadChannel? = null

  override suspend fun execute(data: HttpRequestData): HttpResponseData {
    val range = checkNotNull(data.headers[HttpHeaders.Range])
    ranges += range
    val (start, end) = range.removePrefix("bytes=").split('-').map(String::toInt)
    if (interrupted && start > 0) throw IOException("Disconnected")
    val body = when {
      corrupt -> ByteArray(payload.size)
      ignoreRange -> payload
      else -> payload.copyOfRange(start, end + 1)
    }
    return HttpResponseData(
      if (ignoreRange) HttpStatusCode.OK else HttpStatusCode.PartialContent,
      GMTDate(),
      if (ignoreRange) Headers.Empty else Headers.build {
        append(HttpHeaders.ContentRange, "bytes ${if (wrongRange) start + 1 else start}-$end/${payload.size}")
      },
      HttpProtocolVersion.HTTP_1_1,
      (if (start == 0) firstRangeBody.also { firstRangeBody = null } else null) ?: ByteReadChannel(body),
      callContext(),
    )
  }
}
