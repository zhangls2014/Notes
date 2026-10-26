package me.zhangls.navigation

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
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackdropMotionTest {
  private val compose = createComposeRule()

  @get:Rule val rules = drainMainLooperAfter(compose)
  private val stack = mutableStateListOf(0, 1)
  private lateinit var view: View
  private val maximumTint = Color.Gray.red * 0.24f

  private fun launch() {
    android.provider.Settings.Global.putFloat(
      RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    )
    compose.setContent {
      view = LocalView.current
      Box(Modifier.requiredSize(400.dp, 800.dp).testTag("window")) {
        DeviceCornerNavDisplay(
          entries = (
            stack.map { key ->
              NavEntry(key) {
                Box(Modifier.fillMaxSize().background(if (key == 1) Color.Red else Color.Blue))
              }
            }
            ).toImmutableList(),
          sceneStrategies = (listOf(SinglePaneSceneStrategy<Int>())).toImmutableList(),
          onBack = { stack.removeLast() },
          corners = DeviceCorners(0f, 0f, 0f, 0f),
        )
      }
    }
    compose.onNodeWithTag("window").captureToImage()
    compose.mainClock.autoAdvance = false
  }

  private fun frame(): PixelMap {
    compose.runOnIdle { view.invalidate() }
    compose.mainClock.advanceTimeByFrame()
    compose.waitForIdle()
    return compose.onNodeWithTag("window").captureToImage().toPixelMap()
  }

  @Test fun committedBackdropUsesTheSameOpacityAsThePage() {
    launch()
    try {
      compose.runOnIdle { stack.removeLast() }
      var fadingFrames = 0
      repeat((PREDICTIVE_MOTION_DURATION + PAGE_EXIT_FADE_DURATION) / 16 + 12) {
        val image = frame()
        val centerRed = image[200, 400].red
        val marginRed = image[0, 400].red
        if (centerRed in 0.1f..0.98f) {
          // Over blue, the margin directly measures the gray tint. Remove that tint from
          // the center's red channel to recover the visible red page's actual opacity.
          val pageOpacity = (centerRed - marginRed) / (1f - marginRed)
          assertEquals("Backdrop must fade with the page", maximumTint * pageOpacity, marginRed, 0.01f)
          fadingFrames++
        }
      }
      assertTrue("Inspect multiple frames inside the page fade", fadingFrames >= 3)
      assertTrue("The completed return leaves no backdrop", frame()[0, 400].red < 0.005f)
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  @Test fun cancelledBackdropUsesOnlyTheRestoringPageScale() {
    launch()
    try {
      val dispatcher = checkNotNull(view.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
      compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 400f, 0f, BackEventCompat.EDGE_LEFT)) }
      repeat(3) { frame() }
      compose.runOnIdle {
        dispatcher.dispatchOnBackProgressed(
          BackEventCompat(390f, 400f, 1f, BackEventCompat.EDGE_LEFT),
        )
      }
      repeat(3) { frame() }
      compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
      var restoringFrames = 0
      repeat((PREDICTIVE_MOTION_DURATION + PAGE_EXIT_FADE_DURATION) / 16 + 12) {
        val image = frame()
        val pagePixels = (0 until 400).count { x -> image[x, 400].red > 0.99f }
        val scaleProgress = (400f - pagePixels) / 60f
        if (scaleProgress in 0.2f..0.8f) {
          assertEquals(
            "Backdrop must restore with the page scale",
            maximumTint * scaleProgress,
            image[0, 400].red,
            0.009f,
          )
          restoringFrames++
        }
      }
      assertTrue("Inspect multiple intermediate cancellation frames", restoringFrames >= 3)
      assertEquals(listOf(0, 1), stack.toList())
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }
}
