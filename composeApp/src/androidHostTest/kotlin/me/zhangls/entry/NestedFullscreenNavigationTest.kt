package me.zhangls.entry

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlinx.collections.immutable.toImmutableList
import me.zhangls.about.api.AboutDestination
import me.zhangls.about.api.OpenSourceNoticesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w800dp-h400dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NestedFullscreenNavigationTest {
  private val compose = createComposeRule()

  @get:Rule val rules = drainMainLooperAfter(compose)

  private val stack = mutableStateListOf<NavKey>(HomeDestination, AboutDestination)
  private lateinit var view: View

  private fun launch() {
    android.provider.Settings.Global.putFloat(
      RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    )
    compose.setContent {
      view = LocalView.current
      val entries = stack.map { key ->
        NavEntry(key) {
          val tag = when (key) {
            AboutDestination -> "about"
            is OpenSourceNoticesDestination -> "notices"
            else -> "base"
          }
          Box(Modifier.fillMaxSize().testTag(tag).background(if (key == AboutDestination) Color.Blue else Color.Red))
        }
      }
      ProvideWindowAdaptiveInfo(WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(800, 400), Posture()) {
        ComposeAppTheme {
          Box(Modifier.fillMaxSize().testTag("window")) {
            LayeredNavDisplay(
              entries = entries.toImmutableList(),
              fullscreenEntryCount = stack.size - 1,
              displayKey = HomeDestination,
              contentAlpha = { 1f },
              selected = HomeDestination,
              onSelectTab = {},
              sceneStrategies = listOf(SinglePaneSceneStrategy<NavKey>()).toImmutableList(),
              onBack = { stack.removeLast() },
            )
          }
        }
      }
    }
  }

  @Test fun predictiveReturnRevealsTheFullSizePreviousFullscreenPage() {
    launch()
    compose.onNodeWithTag("about").assertIsDisplayed()
    compose.runOnIdle { stack.add(OpenSourceNoticesDestination()) }
    compose.mainClock.advanceTimeBy(2000)
    compose.waitForIdle()
    compose.onNodeWithTag("notices").assertIsDisplayed().assertWidthIsEqualTo(800.dp)
    val dispatcher = checkNotNull(view.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
    compose.mainClock.autoAdvance = false
    try {
      compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 200f, 0f, BackEventCompat.EDGE_LEFT)) }
      repeat(3) {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
      }
      compose.runOnIdle {
        dispatcher.dispatchOnBackProgressed(BackEventCompat(400f, 200f, 1f, BackEventCompat.EDGE_LEFT))
      }
      compose.mainClock.advanceTimeBy(1500)
      compose.waitForIdle()
      assertEquals(800f, compose.onNodeWithTag("about").fetchSemanticsNode().boundsInRoot.width, 0.5f)
      assertEquals(680f, compose.onNodeWithTag("notices").fetchSemanticsNode().boundsInRoot.width, 0.5f)
      compose.waitUntil(5000) {
        compose.runOnIdle { view.invalidate() }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.onNodeWithTag("window").captureToImage().toPixelMap()[20, 200].blue > 0.7f
      }
      compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
      compose.mainClock.advanceTimeBy(2000)
      compose.waitForIdle()
      compose.onNodeWithTag("notices").assertWidthIsEqualTo(800.dp)
      assertEquals(3, stack.size)
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }
}
