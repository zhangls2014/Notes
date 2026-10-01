package me.zhangls.entry

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.SinglePaneSceneStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeviceCornerNavigationTest {
  @get:Rule val compose = createComposeRule()

  @Test fun predictiveExitClipsPhysicalCornersAndCancellationRestoresPage() {
    assertPredictiveExit(DeviceCorners(60f, 60f, 60f, 60f), true)
  }

  @Test fun absentDeviceCornersKeepMaterialMotionWithoutClipping() {
    assertPredictiveExit(null, false)
  }

  private fun assertPredictiveExit(corners: DeviceCorners?, rounded: Boolean) {
    android.provider.Settings.Global.putFloat(
      org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
    )
    val stack = mutableStateListOf(0, 1)
    lateinit var view: View
    compose.setContent {
      view = LocalView.current
      Box(Modifier.requiredSize(400.dp, 800.dp).testTag("surface")) {
        DeviceCornerNavDisplay(
          entries = stack.map { key -> NavEntry(key) {
            Box(Modifier.fillMaxSize().background(if (key == 1) Color.Red else Color.Blue))
          } },
          sceneStrategies = listOf(SinglePaneSceneStrategy()),
          onBack = { stack.removeLast() },
          corners = corners,
        )
      }
    }
    repeat(3) { compose.mainClock.advanceTimeBy(1000); compose.waitForIdle() }
    compose.onNodeWithTag("surface").captureToImage() // Flush the initial native draw before pausing the clock.
    val dispatcher = checkNotNull(view.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
    compose.mainClock.autoAdvance = false
    try {
      compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT)) }
      compose.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT)) }
      repeat(3) { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }
      compose.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(190f, 400f, 0.5f, BackEventCompat.EDGE_LEFT)) }
      compose.mainClock.advanceTimeByFrame()
      val halfway = compose.onNodeWithTag("surface").captureToImage().toPixelMap()
      // Material's total width reduction is capped at 48dp; even mid-gesture x=30 stays inside.
      assertTrue("Page follows current progress within a frame", halfway[30, 400].red > 0.99f)
      assertTrue("Initial gesture frame must not expose an empty or blended page",
        halfway[0, 400] == Color.Red || halfway[0, 400].blue > 0.85f)
      repeat(3) { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }
      val ready = compose.onNodeWithTag("surface").captureToImage().toPixelMap()
      assertTrue("Previous page has a gray backdrop", ready[0, 400].blue in 0.85f..0.95f)
      // Reverse direction during the same gesture: geometry must reflect the new event next frame.
      compose.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(40f, 400f, 0.1f, BackEventCompat.EDGE_LEFT)) }
      compose.mainClock.advanceTimeByFrame()
      val reversed = compose.onNodeWithTag("surface").captureToImage().toPixelMap()
      assertTrue("Reversal follows the finger without a spring tail", reversed[16, 400].red > 0.99f)
      compose.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(390f, 400f, 1f, BackEventCompat.EDGE_LEFT)) }
      compose.mainClock.advanceTimeBy(1000)
      val preview = compose.onNodeWithTag("surface").captureToImage().toPixelMap()
      // Material sheet scaling removes 48dp of width and 24dp of height, anchored at the page center.
      if (rounded) {
        assertTrue("Rounded corner reveals previous page", preview[29, 53].blue > 0.85f)
      } else {
        assertTrue("Default animation keeps square corners", preview[29, 53].red > 0.9f)
      }
      assertTrue("Page center remains red", preview[200, 400].red > 0.9f)
      compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
      repeat(3) {
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
      }
      val restored = compose.onNodeWithTag("surface").captureToImage().toPixelMap()
      assertTrue("Cancelled gesture restores square page: corner=${restored[2, 2]}, center=${restored[200, 400]}, edge=${restored[65, 125]}", restored[2, 2].red > 0.9f)
      assertEquals(listOf(0, 1), stack.toList())
      compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_RIGHT)) }
      compose.mainClock.advanceTimeBy(100)
      compose.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(390f, 400f, 1f, BackEventCompat.EDGE_RIGHT)) }
      compose.mainClock.advanceTimeBy(1000)
      compose.runOnIdle { dispatcher.onBackPressed() }
      repeat(3) {
        compose.mainClock.advanceTimeBy(1000)
        compose.waitForIdle()
      }
      assertEquals(listOf(0), stack.toList())
      assertTrue(compose.onNodeWithTag("surface").captureToImage().toPixelMap()[200, 400].blue > 0.9f)
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }
}
