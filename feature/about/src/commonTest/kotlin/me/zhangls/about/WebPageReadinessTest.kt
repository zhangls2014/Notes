package me.zhangls.about

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WebPageReadinessTest {
  @Test
  fun waitsForAnExplicitReadyResult() = runTest {
    var checks = 0
    assertTrue(awaitWebPageFrame { ++checks >= 3 })
    assertEquals(3, checks)
    assertEquals(200L, testScheduler.currentTime)
  }

  @Test
  fun timesOutWhenThePageNeverDraws() = runTest {
    assertFalse(awaitWebPageFrame { false })
    assertEquals(45_000L, testScheduler.currentTime)
  }

  @Test
  fun timesOutWhenJavaScriptNeverCallsBack() = runTest {
    assertFalse(awaitWebPageFrame { awaitCancellation() })
    assertEquals(45_000L, testScheduler.currentTime)
  }
}
