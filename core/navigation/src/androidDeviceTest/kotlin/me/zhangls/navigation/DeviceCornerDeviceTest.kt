package me.zhangls.navigation

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.SinglePaneSceneStrategy
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DeviceCornerDeviceTest(private val round: Int) {
  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "round={0}")
    fun rounds(): List<Array<Int>> = (1..5).map { arrayOf(it) }
  }

  @get:Rule val compose = createComposeRule()

  @Test fun predictiveExitClipsPhysicalCornersAndCancellationRestoresPage() {
    assertPredictiveExit(DeviceCorners(60f, 60f, 60f, 60f), true)
  }

  @Test fun absentDeviceCornersUseDefaultClippingAndCancellationRestoresPage() {
    assertPredictiveExit(null, true)
  }

  @Test fun reportedSquareCornersKeepSquareMotion() {
    assertPredictiveExit(DeviceCorners(0f, 0f, 0f, 0f), false)
  }

  @Test fun predictiveBackDuringEntryKeepsThePageOpaque() {
    assertPredictiveExit(DeviceCorners(0f, 0f, 0f, 0f), false, duringEntry = true)
  }

  private fun assertPredictiveExit(corners: DeviceCorners?, rounded: Boolean, duringEntry: Boolean = false) {
    val stack = if (duringEntry) mutableStateListOf(0) else mutableStateListOf(0, 1)
    lateinit var view: View
    compose.setContent {
      view = LocalView.current
      Box(Modifier.requiredSize(400.dp, 800.dp).testTag("surface")) {
        DeviceCornerNavDisplay(
          entries = (
            stack.map { key ->
              NavEntry(key) {
                Box(Modifier.fillMaxSize().testTag("page-$key").background(if (key == 1) Color.Red else Color.Blue))
              }
            }
            ).toImmutableList(),
          sceneStrategies = (listOf(SinglePaneSceneStrategy<Int>())).toImmutableList(),
          onBack = { stack.removeLast() },
          corners = corners,
        )
      }
    }
    repeat(3) {
      advanceMotionBy(1000)
      compose.waitForIdle()
    }
    compose.onNodeWithTag("surface").captureToImage() // Flush the initial native draw before pausing the clock.
    val dispatcher = checkNotNull(view.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
    compose.mainClock.autoAdvance = false
    if (duringEntry) {
      compose.runOnIdle { stack.add(1) }
      repeat(2) { compose.mainClock.advanceTimeByFrame() }
      compose.waitForIdle()
    }
    try {
      assertReversal(dispatcher, view)
      assertCancellation(dispatcher, stack, rounded)
      assertCommit(dispatcher, stack)
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  private fun advanceMotionBy(millis: Long) {
    compose.mainClock.advanceTimeBy(millis * PREDICTIVE_MOTION_DURATION / 220L)
  }

  private fun assertReversal(dispatcher: OnBackPressedDispatcher, view: View) {
    compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT)) }
    compose.runOnIdle {
      dispatcher.dispatchOnBackProgressed(
        BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT),
      )
    }
    repeat(3) {
      compose.mainClock.advanceTimeByFrame()
      compose.waitForIdle()
    }
    compose.runOnIdle {
      dispatcher.dispatchOnBackProgressed(
        BackEventCompat(190f, 400f, 0.5f, BackEventCompat.EDGE_LEFT),
      )
    }
    compose.mainClock.advanceTimeByFrame()
    val halfway = compose.onNodeWithTag("surface").captureToImage().referencePixels()
    // The page stays at least 85% wide; x=60 remains inside throughout the gesture.
    assertTrue("Page follows current progress within a frame", halfway[60, 400].red > 0.99f)
    assertTrue(
      "Initial gesture frame must not expose an empty or blended page",
      halfway[0, 400] == Color.Red || halfway[0, 400].blue > 0.85f,
    )
    repeat(3) {
      compose.mainClock.advanceTimeByFrame()
      compose.waitForIdle()
    }
    compose.runOnIdle { view.invalidate() }
    compose.waitForIdle()
    val ready = compose.onNodeWithTag("surface").captureToImage().referencePixels()
    assertTrue("Previous page has a gray backdrop: ${ready[0, 400]}", ready[0, 400].blue in 0.85f..0.95f)
    // Reverse direction during the same gesture: geometry must reflect the new event next frame.
    compose.runOnIdle {
      dispatcher.dispatchOnBackProgressed(
        BackEventCompat(40f, 400f, 0.1f, BackEventCompat.EDGE_LEFT),
      )
    }
    compose.mainClock.advanceTimeByFrame()
    compose.runOnIdle { view.invalidate() }
    compose.waitForIdle()
    val reversed = compose.onNodeWithTag("surface").captureToImage().referencePixels()
    assertTrue(
      "Round $round reversal follows the finger without a spring tail: " +
        "bounds=${compose.onNodeWithTag("page-1").fetchSemanticsNode().boundsInRoot}, pixel=${reversed[40, 400]}",
      reversed[40, 400].red > 0.99f,
    )
  }

  private fun assertCancellation(dispatcher: OnBackPressedDispatcher, stack: SnapshotStateList<Int>, rounded: Boolean) {
    compose.runOnIdle {
      dispatcher.dispatchOnBackProgressed(
        BackEventCompat(390f, 400f, 1f, BackEventCompat.EDGE_LEFT),
      )
    }
    advanceMotionBy(1000)
    val preview = compose.onNodeWithTag("surface").captureToImage().referencePixels()
    // The 85% page anchors at x=300, with its top-left bound at (45, 60).
    if (rounded) {
      assertTrue("Rounded corner reveals previous page", preview[50, 65].blue > 0.85f)
    } else {
      assertTrue("Default animation keeps square corners", preview[50, 65].red > 0.9f)
    }
    assertTrue("Page center remains red", preview[200, 400].red > 0.9f)
    compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
    repeat(3) {
      advanceMotionBy(1000)
      compose.waitForIdle()
    }
    val restored = compose.onNodeWithTag("surface").captureToImage().referencePixels()
    assertTrue(
      "Cancelled gesture restores square page: corner=${restored[2, 2]}, " +
        "center=${restored[200, 400]}, edge=${restored[65, 125]}",
      restored[2, 2].red > 0.9f,
    )
    assertEquals(listOf(0, 1), stack.toList())
  }

  private fun assertCommit(dispatcher: OnBackPressedDispatcher, stack: SnapshotStateList<Int>) {
    compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_RIGHT)) }
    advanceMotionBy(100)
    compose.runOnIdle {
      dispatcher.dispatchOnBackProgressed(
        BackEventCompat(390f, 400f, 1f, BackEventCompat.EDGE_RIGHT),
      )
    }
    advanceMotionBy(1000)
    compose.runOnIdle { dispatcher.onBackPressed() }
    repeat(3) {
      advanceMotionBy(1000)
      compose.waitForIdle()
    }
    assertEquals(listOf(0), stack.toList())
    assertTrue(compose.onNodeWithTag("surface").captureToImage().referencePixels()[200, 400].blue > 0.9f)
  }
}

// The fixture is 400 × 800 dp; device PixelCopy returns physical pixels.
private fun ImageBitmap.referencePixels(): ReferencePixels = ReferencePixels(toPixelMap(), width / 400f, height / 800f)

private class ReferencePixels(
  private val pixels: PixelMap,
  private val scaleX: Float,
  private val scaleY: Float,
) {
  operator fun get(x: Int, y: Int): Color = pixels[(x * scaleX).toInt(), (y * scaleY).toInt()]
}
