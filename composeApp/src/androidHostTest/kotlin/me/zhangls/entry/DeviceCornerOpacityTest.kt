package me.zhangls.entry

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
import androidx.compose.ui.test.onAllNodesWithTag
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
class DeviceCornerOpacityTest {
  @get:Rule val compose = createComposeRule()

  @Test fun ordinaryPushStaysOpaqueAndPopFadesOut() {
    android.provider.Settings.Global.putFloat(
      org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
    )
    fun advanceMotionBy(millis: Long) = compose.mainClock.advanceTimeBy(millis * PredictiveMotionDuration / 220L)
    // Start inside an existing navigation stack, as for list -> detail navigation.
    val stack = mutableStateListOf("root", "ordinary-list")
    lateinit var view: android.view.View
    compose.setContent {
      view = LocalView.current
      Box(Modifier.requiredSize(400.dp, 800.dp).testTag("surface")) {
        DeviceCornerNavDisplay(
          entries = stack.map { key -> NavEntry(key) {
            Box(Modifier.fillMaxSize().testTag("page-$key").background(if (key == "ordinary-detail") Color.Red else Color.Blue))
          } },
          sceneStrategies = listOf(SinglePaneSceneStrategy()),
          onBack = { stack.removeLast() },
          corners = null,
        )
      }
    }
    repeat(3) { advanceMotionBy(1000); compose.waitForIdle() }
    // Native rendering can present the activity background before the first Compose draw.
    compose.waitUntil(timeoutMillis = 5000) {
      compose.runOnIdle { view.invalidate() }
      compose.onNodeWithTag("surface").captureToImage().toPixelMap()[200, 400] == Color.Blue
    }
    compose.mainClock.autoAdvance = false
    var phase = "initial"
    var expected = Color.Blue
    fun assertOpaque(requireTarget: Boolean = true) {
      compose.runOnIdle { view.invalidate() }
      val image = compose.onNodeWithTag("surface").captureToImage().toPixelMap()
      val uniformColor = image[200, 400]
      assertTrue("Page stays fully opaque ($phase)", uniformColor == Color.Red || uniformColor == Color.Blue)
      if (requireTarget) assertTrue("Target page is displayed ($phase)", uniformColor == expected)
    }
    try {
      assertOpaque()
      phase = "push"
      expected = Color.Red
      compose.runOnIdle { stack.add("ordinary-detail") }
      repeat(5) {
        advanceMotionBy(32)
        compose.waitForIdle()
        assertOpaque(requireTarget = false)
      }
      assertOpaque()
      repeat(3) { advanceMotionBy(1000); compose.waitForIdle() }
      phase = "pop"
      expected = Color.Blue
      compose.runOnIdle { stack.removeLast() }
      var fadingFrames = 0
      var opaqueShrinkingFrames = 0
      var previousRed = 1f
      // The shrink can be slowed independently of the fade; sample both using actual frames.
      repeat((PredictiveMotionDuration + PageExitFadeDuration) / 16 + 8) {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        val pixel = compose.onNodeWithTag("surface").captureToImage().toPixelMap()[200, 400]
        val bounds = compose.onAllNodesWithTag("page-ordinary-detail").fetchSemanticsNodes()
          .firstOrNull()?.boundsInRoot
        if (bounds != null && bounds.width in 340.5f..399.5f) {
          assertTrue("Shrinking finishes before the page starts fading", pixel.red > 0.99f)
          opaqueShrinkingFrames++
        }
        assertTrue("Return fades continuously toward the previous page", pixel.red <= previousRed + 0.01f)
        if (pixel.red in 0.2f..0.9f && pixel.blue > 0.05f) {
          assertTrue("Fading keeps the completed gesture scale", bounds != null &&
            kotlin.math.abs(bounds.width - 340f) < 0.5f)
          fadingFrames++
        }
        previousRed = pixel.red
      }
      assertTrue("Ordinary return includes several fade-out frames", fadingFrames >= 3)
      assertTrue("Ordinary return includes several opaque shrinking frames", opaqueShrinkingFrames >= 3)
      advanceMotionBy(300)
      compose.waitForIdle()
      assertOpaque()
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

}
