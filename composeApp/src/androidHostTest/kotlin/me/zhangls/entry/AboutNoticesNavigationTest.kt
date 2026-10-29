package me.zhangls.entry

import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.BackEventCompat
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.IntSize
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlinx.collections.immutable.toImmutableList
import me.zhangls.about.aboutNavEntry
import me.zhangls.about.api.AboutDestination
import me.zhangls.about.api.OpenSourceNoticesDestination
import me.zhangls.about.api.ReleaseHistoryDestination
import me.zhangls.framework.nav.NavEffect
import me.zhangls.main.api.SettingsDestination
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w800dp-h600dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutNoticesNavigationTest {
  private val compose = createComposeRule()

  @get:Rule val rules = drainMainLooperAfter(compose)

  @Before fun start() {
    initKoin(null)
    provideAboutAppInfo()
  }

  @After fun stop() {
    stopKoin()
  }

  private val stack = NavBackStack<NavKey>(SettingsDestination, AboutDestination)
  private lateinit var rootView: View

  private fun launch(width: Int = 800, posture: Posture = Posture()) {
    android.provider.Settings.Global.putFloat(
      RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    )
    compose.setContent {
      rootView = LocalView.current.rootView
      val provider = entryProvider<NavKey> {
        entry<SettingsDestination> { Box(Modifier.fillMaxSize()) }
        aboutNavEntry(
          paneMetadata = ::noticesPaneMetadata,
          embedProject = { false },
        ) { effect ->
          when (effect) {
            is NavEffect.Navigate -> stack.add(effect.dest)
            NavEffect.Popup -> stack.removeLast()
            else -> error("Unexpected navigation effect: $effect")
          }
        }
      }
      val entries = rememberDecoratedNavEntries(
        backStack = stack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = provider,
      )
      ProvideWindowAdaptiveInfo(WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(width, 600), posture) {
        ComposeAppTheme {
          LayeredNavDisplay(
            entries = entries.toImmutableList(),
            fullscreenEntryCount = stack.size - 1,
            displayKey = SettingsDestination,
            contentAlpha = { 1f },
            selected = SettingsDestination,
            onSelectTab = {},
            sceneStrategies = remember { listOf(SinglePaneSceneStrategy<NavKey>()).toImmutableList() },
            onBack = { stack.removeLast() },
          )
        }
      }
    }
  }

  @Test fun historyUsesNavigationStackAndReturnsToAbout() {
    launch()
    compose.onNodeWithText("Version").performScrollTo().performClick()
    compose.onNodeWithText("View update history").performScrollTo().performClick()
    compose.waitForIdle()
    assertEquals(ReleaseHistoryDestination, stack.last())
    compose.onNodeWithText("Release notes").assertIsDisplayed()
    compose.onNodeWithContentDescription("Back").performClick()
    compose.waitForIdle()
    assertEquals(AboutDestination, stack.last())
    compose.onNodeWithText("About app").assertIsDisplayed()
    compose.onNodeWithText("Build number").assertDoesNotExist()
  }

  @Test fun clickingNoticesMountsWebViewAndPredictiveReturnRevealsAbout() {
    launch()
    compose.onNodeWithText("Open-source software notices").performScrollTo().performClick()
    compose.waitForIdle()
    assertEquals(OpenSourceNoticesDestination(), stack.last())
    compose.runOnIdle {
      val webView = requireNotNull(findWebView(rootView))
      assertTrue(shadowOf(webView).lastLoadedUrl.contains("android.html"))
    }
    val dispatcher = checkNotNull(rootView.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
    compose.mainClock.autoAdvance = false
    try {
      compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 300f, 0f, BackEventCompat.EDGE_LEFT)) }
      repeat(3) {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
      }
      compose.runOnIdle {
        dispatcher.dispatchOnBackProgressed(BackEventCompat(400f, 300f, 0.8f, BackEventCompat.EDGE_LEFT))
      }
      compose.mainClock.advanceTimeBy(1500)
      compose.waitForIdle()
      compose.onNodeWithText("About app").assertIsDisplayed()
      compose.runOnIdle { dispatcher.onBackPressed() }
      compose.mainClock.advanceTimeBy(2000)
      compose.waitForIdle()
      assertEquals(listOf<NavKey>(SettingsDestination, AboutDestination), stack.toList())
      compose.onNodeWithText("About app").assertIsDisplayed()
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  @Test
  @Config(qualifiers = "en-rUS-w1200dp-h600dp-mdpi")
  fun historyPredictiveReturnPreservesHingeColumnsAndScrollAfterCancellation() {
    stack.add(ReleaseHistoryDestination)
    val posture = Posture(hingeList = listOf(HingeInfo(Rect(360f, 0f, 380f, 600f), true, true, false, false)))
    launch(1200, posture)
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodes(hasTestTag("release-1.1.0")).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithContentDescription("Switch to columns").performClick()
    compose.onNodeWithTag("release-history-grid").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 80f) }
    val initial = historyCardGeometry()
    assertTrue("Regression must begin with a scrolled list", initial.second.y < 16f)
    val initialVisibleWidth = compose.onNodeWithTag("release-1.1.0").fetchSemanticsNode().boundsInRoot.width
    val dispatcher = checkNotNull(rootView.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
    compose.mainClock.autoAdvance = false
    try {
      compose.runOnIdle { dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 300f, 0f, BackEventCompat.EDGE_LEFT)) }
      repeat(3) { compose.mainClock.advanceTimeByFrame() }
      for (progress in listOf(0.2f, 0.8f, 0.4f)) {
        compose.runOnIdle {
          dispatcher.dispatchOnBackProgressed(BackEventCompat(400f, 300f, progress, BackEventCompat.EDGE_LEFT))
        }
        compose.mainClock.advanceTimeBy(100)
        compose.waitForIdle()
        assertEquals("Gesture must preserve layout size and scroll position", initial, historyCardGeometry())
        assertTrue(
          "Predictive return must still visually shrink the page",
          compose.onNodeWithTag("release-1.1.0").fetchSemanticsNode().boundsInRoot.width < initialVisibleWidth,
        )
      }
      compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
      compose.mainClock.advanceTimeBy(1500)
      compose.waitForIdle()
      assertEquals(initial, historyCardGeometry())
      assertEquals(ReleaseHistoryDestination, stack.last())
      compose.onNodeWithContentDescription("Switch to single column").assertExists()
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  private fun historyCardGeometry(): Pair<IntSize, Offset> {
    val card = compose.onNodeWithTag("release-1.1.0").fetchSemanticsNode().layoutInfo.coordinates
    val grid = compose.onNodeWithTag("release-history-grid").fetchSemanticsNode().layoutInfo.coordinates
    return card.size to grid.localPositionOf(card, Offset.Zero)
  }

  private fun findWebView(view: View): WebView? = when (view) {
    is WebView -> view
    is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findWebView(view.getChildAt(it)) }
    else -> null
  }
}
