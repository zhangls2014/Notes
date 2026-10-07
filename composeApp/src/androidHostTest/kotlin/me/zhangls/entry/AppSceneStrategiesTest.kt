package me.zhangls.entry

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Transition
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Generic keys exercise the root policy without any Feature destination or implementation. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class AppSceneStrategiesTest {
  @get:Rule val compose = createComposeRule()

  @Test fun narrowPageBelowExpandedHeightUsesWholePageScene() = assertScene(400, 899, "detail")
  @Test fun narrowPageAtExpandedHeightUsesWholePageScene() = assertScene(400, 900, "detail")
  @Test fun tallNarrowPageUsesWholePageScene() = assertScene(400, 1000, "detail")
  @Test fun wideListRetainsAdaptivePlaceholder() = assertScene(900, 1000, "pair", detail = false)
  @Test fun wideDetailUsesAdaptiveScene() = assertScene(900, 1000, "pair")
  @Test fun horizontalHingeRetainsAdaptiveScene() = assertScene(400, 1000, "pair", horizontalHinge = true)
  @Test fun ordinaryPageUsesWholePageSceneInWideWindow() = assertScene(900, 1000, "detail", paneMetadata = false)

  @Test fun resizingRootListDoesNotKeepPageExitAnimationRunning() {
    android.provider.Settings.Global.putFloat(
      org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
    )
    val width = mutableStateOf(400)
    lateinit var rootTransition: Transition<EnterExitState>
    compose.setContent {
      ProvideWindowAdaptiveInfo(
        WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(width.value, 800), Posture(),
      ) {
        DeviceCornerNavDisplay(
          entries = listOf(NavEntry("list", metadata = ListDetailSceneStrategy.listPane(
            sceneKey = "pair", detailPlaceholder = {},
          )) {
            if (width.value == 400) rootTransition = LocalNavAnimatedContentScope.current.transition
            Box(Modifier.fillMaxSize())
          }),
          sceneStrategies = rememberAppSceneStrategies(),
          onBack = {},
          corners = null,
        )
      }
    }
    compose.waitForIdle()
    compose.mainClock.autoAdvance = false
    try {
      compose.runOnIdle { width.value = 900 }
      repeat(8) { compose.mainClock.advanceTimeByFrame() }
      compose.runOnIdle {
        assertEquals("Window layout changes must not animate the root page out", 0L,
          rootTransition.totalDurationNanos)
      }
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  private fun assertScene(
    width: Int,
    height: Int,
    expectedKey: String,
    detail: Boolean = true,
    horizontalHinge: Boolean = false,
    paneMetadata: Boolean = true,
  ) {
    lateinit var scene: Scene<String>
    lateinit var expectedSceneKey: Any
    compose.setContent {
      val posture = if (horizontalHinge) Posture(true, listOf(
        HingeInfo(Rect(0f, 495f, 400f, 505f), false, false, true, false),
      )) else Posture()
      ProvideWindowAdaptiveInfo(
        WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(width, height), posture,
      ) {
        val entries = buildList {
          add(NavEntry("list", metadata = if (paneMetadata)
            ListDetailSceneStrategy.listPane(sceneKey = "pair", detailPlaceholder = {}) else emptyMap()) {
            Box(Modifier.fillMaxSize())
          })
          if (detail) add(NavEntry("detail", metadata = if (paneMetadata)
            ListDetailSceneStrategy.detailPane(sceneKey = "pair") else emptyMap()) {
            Box(Modifier.fillMaxSize())
          })
        }
        expectedSceneKey = if (expectedKey == "detail") entries.last().contentKey else expectedKey
        scene = rememberSceneState(entries, rememberAppSceneStrategies(), onBack = {}).currentScene
      }
    }
    compose.runOnIdle { assertEquals(expectedSceneKey, scene.key) }
  }
}
