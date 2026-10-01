package me.zhangls.entry

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
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
class PageMotionNavigationTest {
  @get:Rule val compose = createComposeRule()

  @Test fun pageUsesOwnSizeWhileNavigationRemainsFixed() {
    android.provider.Settings.Global.putFloat(
      org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
    )
    val stack = mutableStateListOf(0, 1)
    lateinit var view: View
    compose.setContent {
      view = LocalView.current
        Box(Modifier.requiredSize(400.dp, 800.dp).testTag("window")) {
          Box(Modifier.fillMaxSize().padding(start = 80.dp, bottom = 64.dp)) {
            Box(Modifier.fillMaxSize()) {
              DeviceCornerNavDisplay(
                entries = stack.map { key -> NavEntry(key) {
                  Box(Modifier.fillMaxSize().background(if (key == 1) Color.Red else Color.Blue))
                } },
                sceneStrategies = listOf(SinglePaneSceneStrategy()),
                onBack = { stack.removeLast() },
                corners = DeviceCorners(40f, 40f, 40f, 40f),
              )
            }
          }
          Box(Modifier.requiredSize(80.dp, 800.dp).background(Color.Green))
          Box(Modifier.align(Alignment.BottomEnd).requiredSize(320.dp, 64.dp).background(Color.Yellow))
        }
    }
    repeat(3) { compose.mainClock.advanceTimeBy(1000); compose.waitForIdle() }
    val dispatcher = checkNotNull(view.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
    compose.mainClock.autoAdvance = false
    try {
      compose.runOnIdle {
        dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT))
        dispatcher.dispatchOnBackProgressed(BackEventCompat(390f, 400f, 1f, BackEventCompat.EDGE_LEFT))
      }
      repeat(3) { compose.mainClock.advanceTimeBy(500); compose.waitForIdle() }
      val image = compose.onNodeWithTag("window").captureToImage().toPixelMap()
      assertTrue("Page offset is not compensated", image[100, 400].blue in 0.85f..0.95f)
      assertTrue("Backdrop has a gray tint", image[200, 710].red in 0.05f..0.2f &&
        image[200, 710].green in 0.05f..0.2f)
      assertTrue("Page remains opaque", image[110, 400].red > 0.99f)
      assertTrue("Scale uses page width rather than window width", image[380, 400].blue in 0.85f..0.95f)
      assertTrue("Bottom corner reveals the previous page", image[107, 677].blue in 0.85f..0.95f)
      assertTrue("Below the scaled page no background covers the previous page", image[200, 710].blue in 0.85f..0.95f)
      assertTrue("Page bottom center remains opaque", image[200, 670].red > 0.99f)
      assertTrue("Rail remains fixed", image[10, 400].green > 0.99f)
      assertTrue("Bottom navigation remains fixed", image[200, 780] == Color.Yellow)
      compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
      repeat(3) { compose.mainClock.advanceTimeBy(1000); compose.waitForIdle() }
      assertTrue("Cancellation restores the page",
        compose.onNodeWithTag("window").captureToImage().toPixelMap()[100, 400].red > 0.99f)
      // A short drag restores scale quickly, but corner restoration must keep animating.
      compose.runOnIdle {
        dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT))
      }
      repeat(3) { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }
      compose.runOnIdle {
        dispatcher.dispatchOnBackProgressed(BackEventCompat(8f, 400f, 0.02f, BackEventCompat.EDGE_LEFT))
      }
      compose.mainClock.advanceTimeBy(100)
      compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
      compose.mainClock.advanceTimeBy(64)
      compose.waitForIdle()
      val restoring = compose.onNodeWithTag("window").captureToImage().toPixelMap()
      assertTrue("Scale has restored while corners still animate", restoring[90, 400].red > 0.99f)
      assertTrue("Cancellation must not remove corner clipping abruptly", restoring[82, 734].blue > 0.99f)
      compose.mainClock.advanceTimeBy(300)
      compose.waitForIdle()
      assertTrue("Cancellation restores the original square content corner",
        compose.onNodeWithTag("window").captureToImage().toPixelMap()[82, 734].red > 0.99f)
      compose.runOnIdle {
        dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT))
      }
      compose.mainClock.advanceTimeByFrame()
      compose.runOnIdle {
        dispatcher.dispatchOnBackProgressed(BackEventCompat(390f, 400f, 1f, BackEventCompat.EDGE_LEFT))
      }
      repeat(3) { compose.mainClock.advanceTimeBy(500); compose.waitForIdle() }
      compose.runOnIdle { dispatcher.onBackPressed() }
      repeat(3) { compose.mainClock.advanceTimeBy(1000); compose.waitForIdle() }
      assertTrue("Completion displays the previous page",
        compose.onNodeWithTag("window").captureToImage().toPixelMap()[200, 400].blue > 0.99f)
      assertTrue("Completion reveals previous page", stack.toList() == listOf(0))
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }
}
