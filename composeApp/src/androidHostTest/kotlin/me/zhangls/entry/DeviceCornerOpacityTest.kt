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

  @Test fun ordinaryPushAndPopSwitchWithoutMotionOrBlending() {
    android.provider.Settings.Global.putFloat(
      org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
    )
    // Start inside an existing navigation stack, as for list -> detail navigation.
    val stack = mutableStateListOf("root", "ordinary-list")
    lateinit var view: android.view.View
    compose.setContent {
      view = LocalView.current
      Box(Modifier.requiredSize(400.dp, 800.dp).testTag("surface")) {
        DeviceCornerNavDisplay(
          entries = stack.map { key -> NavEntry(key) {
            Box(Modifier.fillMaxSize().background(if (key == "ordinary-detail") Color.Red else Color.Blue))
          } },
          sceneStrategies = listOf(SinglePaneSceneStrategy()),
          onBack = { stack.removeLast() },
          corners = null,
        )
      }
    }
    repeat(3) { compose.mainClock.advanceTimeBy(1000); compose.waitForIdle() }
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
      val uniformColor = image[5, 400]
      assertTrue("Page stays fully opaque ($phase)", uniformColor == Color.Red || uniformColor == Color.Blue)
      if (requireTarget) assertTrue("Target page is displayed ($phase)", uniformColor == expected)
      for (x in 5 until 400 step 10) {
        val pixel = image[x, 400]
        assertTrue("Page switches without motion or blending ($phase) at x=$x: $pixel",
          pixel == uniformColor)
      }
    }
    try {
      assertOpaque()
      phase = "push"
      expected = Color.Red
      compose.runOnIdle { stack.add("ordinary-detail") }
      repeat(5) {
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        assertOpaque(requireTarget = false)
      }
      assertOpaque()
      repeat(3) { compose.mainClock.advanceTimeBy(1000); compose.waitForIdle() }
      phase = "pop"
      expected = Color.Blue
      compose.runOnIdle { stack.removeLast() }
      repeat(5) {
        compose.mainClock.advanceTimeBy(32)
        compose.waitForIdle()
        assertOpaque(requireTarget = false)
      }
      assertOpaque()
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

}
