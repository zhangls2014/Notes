package me.zhangls.entry

import android.view.View
import androidx.activity.BackEventCompat
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlinx.collections.immutable.toImmutableList
import me.zhangls.about.api.AboutDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.navigation.PAGE_EXIT_FADE_DURATION
import me.zhangls.navigation.PREDICTIVE_MOTION_DURATION
import me.zhangls.profile.api.ProfileDestination
import me.zhangls.profile.api.ProfileOrigin
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
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
@Config(sdk = [37], qualifiers = "en-rUS-w800dp-h400dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayeredNavDisplayTest {
  private val compose = createComposeRule()

  @get:Rule val rules = drainMainLooperAfter(compose)

  @Test fun profilePreviewCancellationAndCommitRetainFullViewportAndBaseEntry() {
    assertLayeredReturn(ProfileDestination(ProfileOrigin.Home))
  }

  @Test fun aboutPreviewCancellationAndCommitRetainFullViewportAndBaseEntry() {
    assertLayeredReturn(AboutDestination)
  }

  @Test fun ordinaryReturnKeepsNavigationOnThePageTimeline() {
    assertLayeredReturn(AboutDestination, commitWithGesture = false)
  }

  @Test
  @Config(qualifiers = "en-rUS-w1000dp-h800dp-mdpi")
  fun noticesOverlayShowsListAndDetailTogether() {
    val list = me.zhangls.about.api.OpenSourceNoticesDestination()
    val detail = me.zhangls.about.api.OpenSourceNoticesDestination(me.zhangls.about.api.OpenSourcePage.LICENSES)
    compose.setContent {
      ProvideWindowAdaptiveInfo(WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(1000, 800), Posture()) {
        ComposeAppTheme {
          val entries = listOf(
            NavEntry<NavKey>(HomeDestination) { Box(Modifier.fillMaxSize()) },
            NavEntry<NavKey>(list, metadata = noticesPaneMetadata(list)) {
              Box(Modifier.fillMaxSize().testTag("notices-list"))
            },
            NavEntry<NavKey>(detail, metadata = noticesPaneMetadata(detail)) {
              Box(Modifier.fillMaxSize().testTag("notices-detail"))
            },
          )
          LayeredNavDisplay(
            entries = entries.toImmutableList(),
            fullscreenEntryCount = 2,
            displayKey = HomeDestination,
            contentAlpha = { 1f },
            selected = HomeDestination,
            onSelectTab = {},
            sceneStrategies = rememberAppSceneStrategies<NavKey>().toImmutableList(),
            onBack = {},
          )
        }
      }
    }
    compose.onNodeWithTag("notices-list").assertIsDisplayed()
    compose.onNodeWithTag("notices-detail").assertIsDisplayed()
  }

  private fun assertLayeredReturn(destination: NavKey, commitWithGesture: Boolean = true) {
    android.provider.Settings.Global.putFloat(
      RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    )
    val stack = mutableStateListOf<NavKey>(HomeDestination)
    lateinit var view: View
    var mounts = 0
    var disposals = 0
    var clicks = 0
    var pops = 0
    var baseBounds = Rect.Zero
    compose.setContent {
      view = LocalView.current
      val entries = remember(stack.toList()) {
        stack.map { key ->
          NavEntry(key) {
            if (key == HomeDestination) {
              DisposableEffect(Unit) {
                mounts++
                onDispose { disposals++ }
              }
              Box(
                Modifier.fillMaxSize().onGloballyPositioned { coordinates -> baseBounds = coordinates.boundsInRoot() }
                  .testTag("base").background(Color.Blue).clickable { clicks++ },
              )
            } else {
              Box(Modifier.fillMaxSize().testTag("fullscreen").background(Color.Red))
            }
          }
        }
      }
      ProvideWindowAdaptiveInfo(WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(800, 400), Posture()) {
        ComposeAppTheme {
          Box(Modifier.fillMaxSize().testTag("window")) {
            LayeredNavDisplay(
              entries = (entries).toImmutableList(),
              fullscreenEntryCount = stack.size - 1,
              displayKey = HomeDestination,
              contentAlpha = { 1f },
              selected = HomeDestination,
              onSelectTab = { clicks++ },
              sceneStrategies = (listOf(SinglePaneSceneStrategy<NavKey>())).toImmutableList(),
              onBack = {
                pops++
                stack.removeLast()
              },
            )
          }
        }
      }
    }
    val railColor = compose.onNodeWithTag("window").captureToImage().toPixelMap()[0, 200]
    val railWidth = baseBounds.left
    fun assertNavigationMatchesPage() {
      val page = compose.onNodeWithTag("fullscreen").fetchSemanticsNode().boundsInRoot
      val pageProgress = ((800f - page.width) / 120f).coerceIn(0f, 1f)
      assertEquals("Navigation must use the page's actual progress", pageProgress, baseBounds.left / railWidth, 0.02f)
    }
    compose.mainClock.autoAdvance = false
    try {
      compose.runOnIdle { stack.add(destination) }
      var entryFrames = 0
      repeat(16) {
        compose.mainClock.advanceTimeBy((PREDICTIVE_MOTION_DURATION / 16).toLong())
        compose.waitForIdle()
        if (compose.onAllNodesWithTag("fullscreen").fetchSemanticsNodes().isNotEmpty()) {
          assertNavigationMatchesPage()
          entryFrames++
        } else {
          assertEquals("Navigation stays visible until the page is mounted", railWidth, baseBounds.left, 1f)
        }
      }
      assertTrue("Inspect the page and navigation together through entry", entryFrames >= 12)
    } finally {
      compose.mainClock.autoAdvance = true
    }
    fun settle() {
      compose.mainClock.advanceTimeBy((PREDICTIVE_MOTION_DURATION + PAGE_EXIT_FADE_DURATION + 2000).toLong())
      compose.waitForIdle()
    }
    settle()
    compose.onNodeWithTag("base").assertDoesNotExist()
    compose.onNodeWithTag("fullscreen").assertWidthIsEqualTo(800.dp)
    val dispatcher = checkNotNull(view.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
    compose.mainClock.autoAdvance = false
    fun preview(edge: Int, progress: Float) {
      compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 200f, 0f, edge)) }
      repeat(3) {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
      }
      compose.runOnIdle { dispatcher.dispatchOnBackProgressed(BackEventCompat(400f * progress, 200f, progress, edge)) }
      compose.mainClock.advanceTimeBy(1500)
      compose.waitForIdle()
    }
    fun frame() = run {
      compose.runOnIdle { view.invalidate() }
      // The paused test clock must supply a frame for the invalidation to reach native drawing.
      compose.mainClock.advanceTimeByFrame()
      compose.waitForIdle()
      compose.onNodeWithTag("window").captureToImage().toPixelMap()
    }
    try {
      preview(BackEventCompat.EDGE_LEFT, 0.25f)
      assertNavigationMatchesPage()
      compose.mainClock.advanceTimeBy(500)
      compose.waitForIdle()
      assertNavigationMatchesPage()
      compose.runOnIdle {
        dispatcher.dispatchOnBackProgressed(BackEventCompat(40f, 200f, 0.1f, BackEventCompat.EDGE_LEFT))
      }
      compose.mainClock.advanceTimeByFrame()
      compose.waitForIdle()
      assertNavigationMatchesPage()
      compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
      repeat(16) {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertNavigationMatchesPage()
        assertEquals(
          "Cancellation retains the inset opposite anchor",
          600f,
          compose.onNodeWithTag(
            "fullscreen",
          ).fetchSemanticsNode().boundsInRoot.let { bounds -> bounds.left + bounds.width * 0.75f },
          0.5f,
        )
      }
      settle()
      preview(BackEventCompat.EDGE_LEFT, 1f)
      val bounds = compose.onNodeWithTag("fullscreen").fetchSemanticsNode().boundsInRoot
      assertEquals(
        "Left-edge back anchors at seventy-five percent of the viewport",
        600f,
        bounds.left + bounds.width * 0.75f,
        0.5f,
      )
      assertEquals("Full-screen page reaches eighty-five percent", 680f, bounds.width, 0.5f)
      val expectedRailTint = Color.Gray.red * 0.24f + railColor.red * 0.76f
      var shadedRail = frame()[0, 200]
      // The gesture is held at 100%; wait for native presentation without changing its pose.
      compose.waitUntil(5000) {
        shadedRail = frame()[0, 200]
        kotlin.math.abs(expectedRailTint - shadedRail.red) < 0.03f
      }
      assertEquals("The full-screen backdrop also covers the rail", expectedRailTint, shadedRail.red, 0.03f)
      compose.onNodeWithTag("window").performTouchInput { click(androidx.compose.ui.geometry.Offset(2f, 200f)) }
      assertEquals("Exposed margins must not forward input to the base", 0, clicks)
      compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
      settle()
      compose.onNodeWithTag("fullscreen").assertWidthIsEqualTo(800.dp)
      compose.onNodeWithContentDescription("Home").assertDoesNotExist()
      assertEquals(listOf(HomeDestination, destination), stack.toList())
      assertEquals(1, mounts)
      assertEquals(0, disposals)

      if (commitWithGesture) preview(BackEventCompat.EDGE_RIGHT, 0.5f)
      compose.runOnIdle { dispatcher.onBackPressed() }
      repeat(3) {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
      }
      val exiting = compose.onNodeWithTag("fullscreen").fetchSemanticsNode().boundsInRoot
      assertNavigationMatchesPage()
      if (commitWithGesture) {
        assertEquals(
          "Right-edge commit anchors at twenty-five percent of the viewport",
          200f,
          exiting.left + exiting.width * 0.25f,
          0.5f,
        )
      } else {
        assertEquals("Ordinary return stays centered after cancellation", 400f, exiting.center.x, 0.5f)
      }
      compose.runOnIdle { dispatcher.onBackPressed() }
      assertEquals("Repeated back during exit cannot pop the underlying page", 1, pops)
      repeat(16) {
        compose.mainClock.advanceTimeBy((PREDICTIVE_MOTION_DURATION / 16).toLong())
        compose.waitForIdle()
        if (compose.onAllNodesWithTag("fullscreen").fetchSemanticsNodes().isNotEmpty()) {
          assertNavigationMatchesPage()
        }
      }
      settle()
      compose.onNodeWithTag("fullscreen").assertDoesNotExist()
      compose.onNodeWithTag("base").assertIsDisplayed()
      compose.onNodeWithContentDescription("Home").assertIsDisplayed()
      compose.onNodeWithTag("base").performClick()
      assertEquals(1, clicks)
      assertEquals(1, mounts)
      assertEquals(0, disposals)
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }
}
