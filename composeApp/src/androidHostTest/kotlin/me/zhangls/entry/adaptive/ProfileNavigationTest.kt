package me.zhangls.entry.adaptive

import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.material3.adaptive.Posture
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
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
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
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
  @get:Rule val compose = createComposeRule()
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
    compose.waitUntil(10_000) {
      compose.onAllNodesWithText("Adaptive review").fetchSemanticsNodes().isNotEmpty()
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
}
