@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package me.zhangls.download

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSRunLoop
import platform.Foundation.NSURL
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionTaskStateSuspended
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class BackgroundDownloadCancellationTest {
  @Test
  fun cancelsOnlyObsoleteSiteTasksWithoutRetryingCancellationCallbacks() = runWithMainLoop {
    val owner = BackgroundDownloads(NSURLSessionConfiguration.ephemeralSessionConfiguration)
    fun request(directory: String, file: String) = DownloadRequest(
      "https://downloads.example.test/$file",
      "/cache/notes-web-assets/$directory/$file",
      1,
      "a".repeat(64),
    )
    val old = request("one.example.test-443", "old.wasm")
    val current = request("one.example.test-443", "current.wasm")
    val other = request("two.example.test-443", "old.wasm")
    fun task(request: DownloadRequest) = owner.session
      .downloadTaskWithURL(checkNotNull(NSURL.URLWithString(request.url))).also {
        it.taskDescription = Json.encodeToString(RangeTask(request, 0))
        it.resume()
        it.suspend()
      }
    val oldTask = task(old)
    val currentTask = task(current)
    val otherTask = task(other)
    try {
      assertTrue(
        owner.hasActiveDownload("notes-web-assets/one.example.test-443/old.wasm"),
        "Fixture must be registered",
      )
      assertTrue(withTimeout(5_000) { owner.cancelObsolete(listOf(current)) })
      assertFalse(owner.hasActiveDownload("notes-web-assets/one.example.test-443/old.wasm"))
      assertEquals(NSURLSessionTaskStateSuspended, currentTask.state)
      assertEquals(NSURLSessionTaskStateSuspended, otherTask.state)
      owner.URLSession(owner.session, task = oldTask, didCompleteWithError = NSError("test", -999, null))
      assertFalse(owner.hasActiveDownload("notes-web-assets/one.example.test-443/old.wasm"))
    } finally {
      val emptyTargets = listOf(current, other).map { it.copy(destination = it.destination + ".keep") }
      withTimeout(5_000) { assertTrue(owner.cancelObsolete(emptyTargets)) }
      owner.session.invalidateAndCancel()
    }
  }
}

private fun runWithMainLoop(action: suspend () -> Unit) {
  var failure: Throwable? = null
  val job = CoroutineScope(Dispatchers.Main).launch {
    try {
      action()
    } catch (error: Throwable) {
      failure = error
    }
  }
  val start = TimeSource.Monotonic.markNow()
  try {
    while (!job.isCompleted && start.elapsedNow() < 10.seconds) {
      NSRunLoop.mainRunLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.01))
    }
    assertTrue(job.isCompleted, "Native cancellation must complete within the test deadline")
    failure?.let { throw it }
  } finally {
    job.cancel()
  }
}
