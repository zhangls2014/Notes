@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package me.zhangls.download

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadDelegateProtocol
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSURLSessionTask
import platform.Foundation.setValue
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val sessionId = "${NSBundle.mainBundle.bundleIdentifier}.download.ranges.v1"
private val backgroundDownloads by lazy { BackgroundDownloads() }

/** Schedules the remaining ranges in one system-owned background URLSession. */
suspend fun downloadInBackground(request: DownloadRequest, onProgress: (Long) -> Unit) {
  withDownloadTarget(Path(request.destination)) {
    withContext(Dispatchers.Main) { backgroundDownloads.download(request, onProgress) }
  }
}

/** Interleaves the missing fragments of independently progressing files in the system queue. */
suspend fun prepareBackgroundDownloads(requests: List<DownloadRequest>) {
  require(requests.size <= 3)
  withContext(Dispatchers.Main) { backgroundDownloads.prepare(requests) }
}

suspend fun hasActiveBackgroundDownload(relativePath: String): Boolean =
  withContext(Dispatchers.Main) { backgroundDownloads.hasActiveDownload(relativePath) }

/** Pass the UIKit background URLSession callback to the transfer owner. */
fun handleDownloadSessionEvents(identifier: String, completion: () -> Unit) {
  if (identifier == sessionId) backgroundDownloads.handleEvents(completion) else completion()
}

@Serializable
internal data class RangeTask(val request: DownloadRequest, val start: Long, val attempt: Int = 0) {
  val length get() = minOf(FRAGMENT_BYTES, request.size - start)
}

internal class DownloadFiles(val request: DownloadRequest) {
  val target = Path(request.destination)
  val parent = requireNotNull(target.parent)
  val partial = Path(parent, checkpointName(target.name, request.size, request.sha256))
  val full = Path(parent, "${partial.name}.full")

  fun fragment(start: Long) = Path(parent, "${partial.name}.$start")
  fun ranges() = (0L until request.size step FRAGMENT_BYTES).map { RangeTask(request, it) }

  fun prefixSize(): Long = checkpointOffset(SystemFileSystem.metadataOrNull(partial)?.size ?: 0, request.size)

  fun contains(part: RangeTask, prefix: Long) =
    part.start < prefix || SystemFileSystem.metadataOrNull(fragment(part.start))?.size == part.length

  fun verified(path: Path): Boolean =
    SystemFileSystem.metadataOrNull(path)?.size == request.size &&
      downloadSha256(path.toString()) == request.sha256

  fun publish(fromFullResponse: Boolean) {
    if (!fromFullResponse) {
      val prefix = prefixSize()
      if (SystemFileSystem.exists(partial)) truncateDownload(partial.toString(), prefix)
      if (prefix < request.size) SystemFileSystem.sink(partial, append = true).buffered().use { sink ->
        for (part in ranges().filter { it.start >= prefix }) {
          SystemFileSystem.source(fragment(part.start)).buffered().use { sink.transferFrom(it) }
        }
      }
    }
    val candidate = if (fromFullResponse) full else partial
    if (!verified(candidate)) {
      SystemFileSystem.delete(candidate, mustExist = false)
      ranges().forEach { SystemFileSystem.delete(fragment(it.start), mustExist = false) }
      throw IOException("Resource size or SHA-256 does not match")
    }
    SystemFileSystem.atomicMove(candidate, target)
    if (fromFullResponse) SystemFileSystem.delete(partial, mustExist = false)
    ranges().forEach { SystemFileSystem.delete(fragment(it.start), mustExist = false) }
  }
}

private class BackgroundDownloads : NSObject(), NSURLSessionDownloadDelegateProtocol {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val session by lazy {
    val configuration = NSURLSessionConfiguration.backgroundSessionConfigurationWithIdentifier(sessionId)
    configuration.sessionSendsLaunchEvents = true
    NSURLSession.sessionWithConfiguration(configuration, this, NSOperationQueue.mainQueue)
  }
  private data class Observer(val completed: CompletableDeferred<Unit>, val progress: (Long) -> Unit)
  private val observers = mutableMapOf<DownloadRequest, Observer>()
  private val received = mutableMapOf<Pair<DownloadRequest, Long>, Long>()
  private val enqueued = mutableSetOf<Pair<DownloadRequest, Long>>()
  private val publishing = mutableSetOf<DownloadRequest>()
  private val published = mutableSetOf<DownloadRequest>()
  private var completionHandler: (() -> Unit)? = null
  private var eventsFinished = false

  fun handleEvents(completion: () -> Unit) {
    eventsFinished = false
    completionHandler = completion
    // Reconnect by the same identifier even when no app screen has launched.
    session
  }

  suspend fun prepare(requests: List<DownloadRequest>) {
    val pending = currentTasks().mapNotNull { it.rangeTask() }.mapTo(mutableSetOf()) { it.request to it.start }
    val missing = withContext(Dispatchers.IO) {
      requests.mapNotNull { request ->
        val files = DownloadFiles(request)
        if (files.verified(files.target)) return@mapNotNull null
        SystemFileSystem.createDirectories(files.parent)
        val prefix = files.prefixSize()
        files.ranges().filter { !files.contains(it, prefix) && (request to it.start) !in pending }
      }
    }
    for (part in interleaveRanges(missing)) {
      val files = DownloadFiles(part.request)
      if (part.request !in publishing && part.request !in published && !files.contains(part, files.prefixSize())) {
        enqueue(part)
      }
    }
  }

  suspend fun download(request: DownloadRequest, progress: (Long) -> Unit) {
    require(Path(request.destination).isAbsolute && request.url.startsWith("https://") &&
      request.size > 0 && request.sha256.matches(Regex("[a-f0-9]{64}")))
    observers[request]?.let { existing -> return existing.completed.await() }
    val files = DownloadFiles(request)
    if (withContext(Dispatchers.IO) { files.verified(files.target) }) return
    published -= request
    observers[request]?.let { existing -> return existing.completed.await() }
    val observer = Observer(CompletableDeferred(), progress)
    observers[request] = observer
    try {
      SystemFileSystem.createDirectories(files.parent)
      val pending = currentTasks().mapNotNull { it.rangeTask() }.filter { it.request == request }
      progress(files.prefixSize())
      if (!publishIfReady(files)) {
        val prefix = files.prefixSize()
        for (part in files.ranges()) {
          if (!files.contains(part, prefix) && pending.none { it.start == part.start }) enqueue(part)
        }
      }
      report(files)
      observer.completed.await()
    } finally {
      if (observers[request] == observer) observers.remove(request)
    }
  }

  private suspend fun currentTasks(): List<NSURLSessionTask> = suspendCoroutine { continuation ->
    session.getAllTasksWithCompletionHandler { tasks ->
      continuation.resume(tasks.orEmpty().filterIsInstance<NSURLSessionTask>())
    }
  }

  suspend fun hasActiveDownload(relativePath: String): Boolean {
    val suffix = "/$relativePath"
    return currentTasks().any { it.rangeTask()?.request?.destination?.endsWith(suffix) == true } ||
      enqueued.any { (request, _) -> request.destination.endsWith(suffix) } ||
      publishing.any { it.destination.endsWith(suffix) }
  }

  private fun enqueue(part: RangeTask) {
    if (!enqueued.add(part.request to part.start)) return
    val url = checkNotNull(NSURL.URLWithString(part.request.url))
    val request = NSMutableURLRequest.requestWithURL(url)
    request.setValue("bytes=${part.start}-${part.start + part.length - 1}", "Range")
    request.setValue("identity", "Accept-Encoding")
    val task = session.downloadTaskWithRequest(request)
    task.taskDescription = Json.encodeToString(part)
    task.resume()
  }

  override fun URLSession(
    session: NSURLSession,
    downloadTask: NSURLSessionDownloadTask,
    didWriteData: Long,
    totalBytesWritten: Long,
    totalBytesExpectedToWrite: Long,
  ) {
    val part = downloadTask.rangeTask() ?: return
    if (part.request !in observers) return
    received[part.request to part.start] = totalBytesWritten.coerceIn(0, part.length)
    report(DownloadFiles(part.request))
  }

  override fun URLSession(
    session: NSURLSession,
    downloadTask: NSURLSessionDownloadTask,
    didFinishDownloadingToURL: NSURL,
  ) {
    val part = downloadTask.rangeTask() ?: return
    if (part.request in publishing || part.request in published) return
    val files = DownloadFiles(part.request)
    received.remove(part.request to part.start)
    try {
      val response = downloadTask.response as? NSHTTPURLResponse ?: throw IOException("Missing HTTP response")
      val encoding = response.header("Content-Encoding")
      if (response.URL?.absoluteString != part.request.url ||
        (encoding != null && !encoding.equals("identity", ignoreCase = true))
      ) throw IOException("Unexpected download origin or content encoding")
      val source = Path(checkNotNull(didFinishDownloadingToURL.path))
      val actualSize = SystemFileSystem.metadataOrNull(source)?.size
      when (response.statusCode.toInt()) {
        206 -> {
          val expectedRange = "bytes ${part.start}-${part.start + part.length - 1}/${part.request.size}"
          if (actualSize != part.length || !expectedRange.equals(response.header("Content-Range"), true)) {
            throw IOException("Unexpected Content-Range or fragment length")
          }
          SystemFileSystem.atomicMove(source, files.fragment(part.start))
        }
        200 -> {
          if (actualSize != part.request.size) throw IOException("Unexpected full response length")
          SystemFileSystem.atomicMove(source, files.full)
        }
        408, 429, in 500..599 -> return
        else -> throw IOException("HTTP ${response.statusCode}")
      }
      report(files)
      publishIfReady(files)
    } catch (failure: Exception) {
      observers[part.request]?.completed?.completeExceptionally(failure)
    }
  }

  override fun URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError: NSError?) {
    val part = task.rangeTask() ?: return
    enqueued.remove(part.request to part.start)
    val status = (task.response as? NSHTTPURLResponse)?.statusCode?.toInt()
    if (didCompleteWithError == null && status != 408 && status != 429 && status !in 500..599) return
    received.remove(part.request to part.start)
    if (part.request !in publishing) retry(part)
  }

  private fun retry(part: RangeTask) {
    val files = DownloadFiles(part.request)
    if (part.request in published || files.contains(part, files.prefixSize())) return
    if (part.attempt < 2) enqueue(part.copy(attempt = part.attempt + 1))
    else observers[part.request]?.completed?.completeExceptionally(IOException("Background fragment interrupted"))
  }

  private fun report(files: DownloadFiles) {
    val observer = observers[files.request] ?: return
    val prefix = files.prefixSize()
    val count = files.ranges().sumOf { part ->
      if (files.contains(part, prefix)) part.length else received[files.request to part.start] ?: 0
    }
    observer.progress(minOf(count, files.request.size))
  }

  private fun publishIfReady(files: DownloadFiles): Boolean {
    val request = files.request
    if (request in publishing) return true
    val full = SystemFileSystem.metadataOrNull(files.full)?.size == request.size
    val prefix = files.prefixSize()
    if (!full && !files.ranges().all { files.contains(it, prefix) }) return false
    publishing += request
    observers[request]?.progress?.invoke(request.size)
    scope.launch {
      try {
        withContext(Dispatchers.IO) { files.publish(full) }
        published += request
        if (full) currentTasks().filter { it.rangeTask()?.request == request }.forEach { it.cancel() }
        observers[request]?.completed?.complete(Unit)
      } catch (failure: Exception) {
        observers[request]?.completed?.completeExceptionally(failure)
      } finally {
        publishing -= request
        received.keys.removeAll { it.first == request }
        completeSessionEvents()
      }
    }
    return true
  }

  override fun URLSessionDidFinishEventsForBackgroundURLSession(session: NSURLSession) {
    eventsFinished = true
    completeSessionEvents()
  }

  private fun completeSessionEvents() {
    val completion = completionHandler ?: return
    if (!eventsFinished || publishing.isNotEmpty()) return
    completionHandler = null
    eventsFinished = false
    completion()
  }
}

private fun NSHTTPURLResponse.header(name: String): String? =
  allHeaderFields.entries.firstOrNull { (it.key as? String)?.equals(name, ignoreCase = true) == true }?.value as? String

private fun NSURLSessionTask.rangeTask(): RangeTask? =
  taskDescription?.let { runCatching { Json.decodeFromString<RangeTask>(it) }.getOrNull() }


internal fun interleaveRanges(ranges: List<List<RangeTask>>): List<RangeTask> = buildList {
  val rounds = ranges.maxOfOrNull { it.size } ?: 0
  for (index in 0 until rounds) {
    for (file in ranges) file.getOrNull(index)?.let(::add)
  }
}
