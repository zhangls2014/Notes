package me.zhangls.entry.adaptive

import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.material3.adaptive.Posture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.entry.AppNavHost
import me.zhangls.entry.AppViewModel
import me.zhangls.entry.initKoin
import me.zhangls.entry.drainMainLooperAfter
import me.zhangls.main.AppShell
import me.zhangls.main.api.HomeDestination
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProfileNavigationTest {
  private val compose = createComposeRule()
  @get:Rule val rules = drainMainLooperAfter(compose)
  private lateinit var composeView: android.view.View
  private val users = AdaptiveUsers().apply {
    userFlow.value = UserModel("local", "Ada Lovelace")
  }

  @Before fun start() {
    initKoin {
      modules(module {
        single<android.content.Context> { RuntimeEnvironment.getApplication() }
        single<UserRepository> { users }
        single<SettingsRepository> { AdaptiveSettings() }
        single<EmailsRepository> { AdaptiveEmails() }
      })
    }
  }

  @After fun stop() { stopKoin() }

  private fun launch(
    restoration: StateRestorationTester = StateRestorationTester(compose),
    width: Int = 400,
    height: Int = 800,
  ) {
    restoration.setContent {
      composeView = androidx.compose.ui.platform.LocalView.current
      ProvideWindowAdaptiveInfo(
        WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(width, height), Posture(),
      ) {
        ComposeAppTheme {
          AppNavHost(viewModel = org.koin.compose.viewmodel.koinViewModel<AppViewModel>())
        }
      }
    }
    try {
      compose.waitUntil(10_000) {
        compose.onAllNodesWithText("Adaptive review").fetchSemanticsNodes().isNotEmpty()
      }
    } catch (failure: ComposeTimeoutException) {
      throw AssertionError(compose.onRoot().printToString(), failure)
    }
  }

  private fun assertProfile() {
    repeat(3) {
      compose.mainClock.advanceTimeBy(1000)
      compose.waitForIdle()
    }
    compose.onNodeWithText("Personal information").assertIsDisplayed()
    compose.onNodeWithText("Ada Lovelace").assertIsDisplayed()
    compose.onNodeWithText("Change avatar").assertHasClickAction().assertIsEnabled()
    listOf("Home", "Favorites", "Settings").forEach {
      compose.onNodeWithContentDescription(it).assertDoesNotExist()
    }
  }

  @Test fun settingsEntryOpensProfileAndRestoresBackStack() {
    val restoration = StateRestorationTester(compose)
    launch(restoration)
    compose.onNodeWithText("Settings").performClick()
    compose.onNodeWithText("Personal information").performClick()
    assertProfile()
    if (java.lang.Boolean.getBoolean("notes.screenshots.candidates")) {
      compose.onAllNodes(isRoot()).onFirst().captureRoboImage("build/adaptive-candidates/profile.png")
    }
    restoration.emulateSavedInstanceStateRestore()
    assertProfile()
    compose.onNodeWithContentDescription("Back").performClick()
    compose.onNodeWithText("Personal information").assertHasClickAction()
    compose.onNodeWithText("Change avatar").assertDoesNotExist()
    compose.onNodeWithContentDescription("Settings").assertIsSelected()
  }

  @Test
  @Config(qualifiers = "en-rUS-w1200dp-h900dp-mdpi")
  fun wideProfileHidesRailAndSystemBackRestoresSettings() {
    launch(width = 1200, height = 900)
    compose.onNodeWithContentDescription("Settings").performClick()
    compose.onNodeWithText("Personal information").performClick()
    assertProfile()
    compose.runOnIdle {
      checkNotNull(composeView.findViewTreeOnBackPressedDispatcherOwner())
        .onBackPressedDispatcher.onBackPressed()
    }
    compose.mainClock.advanceTimeBy(1000)
    compose.waitForIdle()
    compose.onNodeWithText("Personal information").assertHasClickAction()
    compose.onNodeWithContentDescription("Settings").assertIsSelected().assertIsDisplayed()
  }

  @Test fun expandedSearchAvatarReturnsToCollapsedSearch() {
    launch()
    compose.onNodeWithContentDescription("Search").performTouchInput { click() }
    compose.mainClock.advanceTimeBy(1000)
    compose.waitForIdle()
    compose.onAllNodes(hasSetTextAction()).onLast().performTextReplacement("Adaptive")
    compose.onAllNodesWithContentDescription("Profile").onLast().performClick()
    assertProfile()
    compose.onNodeWithContentDescription("Back").performClick()
    compose.onNodeWithText("Adaptive review").assertIsDisplayed()
    compose.onNodeWithContentDescription("Close search").assertDoesNotExist()
  }

  @Test fun searchAvatarOpensProfileAndBackReturnsToMail() {
    launch()
    compose.onNodeWithContentDescription("Profile").assertHeightIsAtLeast(48.dp).assertHasClickAction().performClick()
    assertProfile()
    compose.onNodeWithContentDescription("Back").performClick()
    compose.onNodeWithText("Adaptive review").assertIsDisplayed()
  }

  @Test
  @Config(qualifiers = "en-rUS-w800dp-h400dp-mdpi")
  fun landscapeProfileReturnStartsFromFullWindow() {
    android.provider.Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    launch(width = 800, height = 400)
    compose.onNodeWithContentDescription("Settings").performClick()
    compose.onNodeWithText("Personal information").performClick()
    assertProfile()
    val start = compose.onNodeWithText("Personal information").fetchSemanticsNode().boundsInRoot
    compose.mainClock.autoAdvance = false
    try {
      compose.onNodeWithContentDescription("Back").performClick()
      compose.mainClock.advanceTimeBy(32)
      compose.waitForIdle()
      val exiting = compose.onAllNodesWithText("Personal information").fetchSemanticsNodes()
        .minBy { it.boundsInRoot.top }.boundsInRoot
      assertTrue("Returning full-screen page must not jump past the rail: $start -> $exiting",
        exiting.left - start.left < 40f)
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  @Test
  @Config(qualifiers = "en-rUS-w800dp-h400dp-mdpi")
  fun navigationRailHidesAndShowsThroughIntermediatePositions() = assertNavigationMotion(800, 400)

  @Test fun bottomNavigationHidesAndShowsThroughIntermediatePositions() = assertNavigationMotion(400, 800)

  @Test
  @Config(qualifiers = "en-rUS-w800dp-h400dp-mdpi")
  fun rightNavigationRailTracksTheSameProgress() = assertNavigationMotion(800, 400, rtl = true)

  private fun assertNavigationMotion(width: Int, height: Int, rtl: Boolean = false) {
    android.provider.Settings.Global.putFloat(RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val progress = mutableFloatStateOf(1f)
    compose.setContent {
      ProvideWindowAdaptiveInfo(WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(width, height), Posture()) {
        CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
          ComposeAppTheme {
            AppShell(HomeDestination, {}, navigationProgress = { progress.floatValue }) {
              Box(Modifier.fillMaxSize().testTag("shell-content"))
            }
          }
        }
      }
    }
    fun extent(): Float {
      val bounds = compose.onNodeWithTag("shell-content").fetchSemanticsNode().boundsInRoot
      return if (width > height) bounds.width else bounds.height
    }
    val visible = extent()
    val full = if (width > height) width.toFloat() else height.toFloat()
    assertTrue(visible < full)
    compose.mainClock.autoAdvance = false
    try {
      fun sampleMotion(fractions: List<Float>): List<Float> = fractions.map { fraction ->
        compose.runOnIdle { progress.floatValue = fraction }
        repeat(2) { compose.mainClock.advanceTimeByFrame(); compose.waitForIdle() }
        val actual = extent()
        org.junit.Assert.assertEquals("Layout must directly track progress=$fraction",
          full - (full - visible) * fraction, actual, 1f)
        if (width > height) {
          val bounds = compose.onNodeWithTag("shell-content").fetchSemanticsNode().boundsInRoot
          org.junit.Assert.assertEquals("Rail must stay on the layout's Start side",
            if (rtl) 0f else full - actual, bounds.left, 1f)
        }
        actual
      }
      val hiding = sampleMotion(listOf(0.75f, 0.5f, 0.25f, 0f))
      assertTrue("Navigation must slide out through intermediate sizes: $hiding", hiding.any { it > visible && it < full })
      org.junit.Assert.assertEquals(full, extent(), 0.5f)
      val showing = sampleMotion(listOf(0.25f, 0.5f, 0.75f, 1f))
      assertTrue("Navigation must slide in through intermediate sizes: $showing", showing.any { it > visible && it < full })
      org.junit.Assert.assertEquals(visible, extent(), 0.5f)
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }
}
