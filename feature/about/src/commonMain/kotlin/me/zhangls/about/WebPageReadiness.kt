package me.zhangls.about

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val PAGE_FRAME_TIMEOUT_MILLIS = 45_000L
private const val PAGE_FRAME_POLL_MILLIS = 100L

internal const val WEB_PAGE_FRAME_SCRIPT = """
  document.getElementById('notes-app')?.getAttribute('data-page-frame') === 'ready' ? 'ready' : 'waiting'
"""

internal suspend fun awaitWebPageFrame(isReady: suspend () -> Boolean): Boolean =
  withTimeoutOrNull(PAGE_FRAME_TIMEOUT_MILLIS) {
    while (!isReady()) delay(PAGE_FRAME_POLL_MILLIS)
    true
  } ?: false
