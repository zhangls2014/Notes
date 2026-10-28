package me.zhangls.entry

import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.Posture
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlinx.collections.immutable.toImmutableList
import me.zhangls.about.api.OpenSourceNoticesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w800dp-h400dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FullscreenAndroidViewTouchTest {
  private val compose = createComposeRule()

  @get:Rule val rules = drainMainLooperAfter(compose)
  private val events = mutableListOf<Int>()

  @Test fun fullscreenNativeViewReceivesTheWholeDragWithoutCancellation() {
    compose.setContent {
      val entries = listOf(
        NavEntry<NavKey>(HomeDestination) { Box(Modifier.fillMaxSize()) },
        NavEntry<NavKey>(OpenSourceNoticesDestination()) {
          AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
              View(context).apply {
                setOnTouchListener { _, event ->
                  events += event.actionMasked
                  true
                }
              }
            },
          )
        },
      )
      ProvideWindowAdaptiveInfo(WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(800, 400), Posture()) {
        ComposeAppTheme {
          Box(Modifier.fillMaxSize().testTag("window")) {
            LayeredNavDisplay(
              entries = entries.toImmutableList(),
              fullscreenEntryCount = 1,
              displayKey = HomeDestination,
              contentAlpha = { 1f },
              selected = HomeDestination,
              onSelectTab = {},
              sceneStrategies = listOf(SinglePaneSceneStrategy<NavKey>()).toImmutableList(),
              onBack = {},
            )
          }
        }
      }
    }
    compose.onNodeWithTag("window").performTouchInput { swipeUp() }
    compose.runOnIdle {
      assertEquals(MotionEvent.ACTION_DOWN, events.first())
      assertTrue("Native scrolling must receive movement events", MotionEvent.ACTION_MOVE in events)
      assertEquals("The parent must not cancel the native drag", 0, events.count { it == MotionEvent.ACTION_CANCEL })
      assertEquals(MotionEvent.ACTION_UP, events.last())
    }
  }
}
