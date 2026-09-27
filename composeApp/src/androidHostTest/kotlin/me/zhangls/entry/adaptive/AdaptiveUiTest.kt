package me.zhangls.entry.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.ui.geometry.Rect
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import com.github.takahirom.roborazzi.captureRoboImage
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
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Real navigation and feature implementations, with deterministic repositories and no device services. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w1600dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AdaptiveUiTest(private val scenario: Scenario) {
  data class Scenario(
    val name: String,
    val width: Int,
    val height: Int,
    val fontScale: Float = 1f,
    val dark: Boolean = false,
    val fold: String? = null,
  ) {
    override fun toString() = name
  }

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    fun scenarios() = buildList {
      for (width in listOf(400, 610, 900)) for (height in listOf(400, 500, 1000)) {
        add(arrayOf(Scenario("${width}x$height", width, height)))
      }
      add(arrayOf(Scenario("desktop", 1600, 1000)))
      add(arrayOf(Scenario("large-font", 400, 500, fontScale = 1.5f)))
      add(arrayOf(Scenario("dark", 400, 500, dark = true)))
      add(arrayOf(Scenario("book-half", 900, 1000, fold = "book-half")))
      add(arrayOf(Scenario("book-flat", 900, 1000, fold = "book-flat")))
      add(arrayOf(Scenario("tabletop-half", 900, 1000, fold = "tabletop-half")))
      add(arrayOf(Scenario("tabletop-flat", 900, 1000, fold = "tabletop-flat")))
    }
  }

  private val compose = createComposeRule()
  @get:Rule val rules: org.junit.rules.TestRule = org.junit.rules.RuleChain.outerRule(
    org.junit.rules.TestRule { base, _ -> object : org.junit.runners.model.Statement() {
      override fun evaluate() {
        org.robolectric.RuntimeEnvironment.setQualifiers("en-rUS-w${scenario.width}dp-h${scenario.height}dp-mdpi")
        android.provider.Settings.Global.putFloat(
          org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
          android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
          0f,
        )
        base.evaluate()
      }
    } }
  ).around(compose)
  private val users = AdaptiveUsers()
  private var width by mutableStateOf(scenario.width)
  private var flat by mutableStateOf(scenario.fold?.endsWith("flat") == true)
  private lateinit var focusManager: androidx.compose.ui.focus.FocusManager

  @Before fun start() {
    initKoin {
      modules(module {
        single<android.content.Context> { org.robolectric.RuntimeEnvironment.getApplication() }
        single<UserRepository> { users }
        single<SettingsRepository> { AdaptiveSettings() }
        single<EmailsRepository> { AdaptiveEmails() }
      })
    }
  }

  @After fun stop() { stopKoin() }

  private fun launch() {
    compose.setContent {
      focusManager = androidx.compose.ui.platform.LocalFocusManager.current
      Box(Modifier.fillMaxSize()) {
        Box(Modifier.requiredSize(width.dp, scenario.height.dp).testTag("screen")) {
          ProvideWindowAdaptiveInfo(
            WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(width, scenario.height),
            posture(),
          ) {
            ComposeAppTheme(darkTheme = scenario.dark, fontScale = scenario.fontScale) {
              AppNavHost(viewModel = org.koin.compose.viewmodel.koinViewModel<AppViewModel>())
            }
          }
        }
      }
    }
  }

  private fun posture(): Posture {
    val fold = scenario.fold ?: return Posture()
    val vertical = fold.startsWith("book")
    val bounds = if (vertical) Rect(width / 2f - 5, 0f, width / 2f + 5, scenario.height.toFloat())
      else Rect(0f, scenario.height / 2f - 5, width.toFloat(), scenario.height / 2f + 5)
    return Posture(!vertical && !flat, listOf(HingeInfo(bounds, flat, vertical, !flat, false)))
  }

  private fun waitFor(text: String) {
    try {
      compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    } catch (failure: androidx.compose.ui.test.ComposeTimeoutException) {
      compose.onAllNodes(isRoot()).fetchSemanticsNodes().indices.forEach {
        println(compose.onAllNodes(isRoot())[it].printToString())
      }
      throw failure
    }
  }

  private fun imagePath(screen: String): String {
    val directory = if (java.lang.Boolean.getBoolean("notes.screenshots.candidates")) {
      "build/adaptive-candidates"
    } else {
      "src/androidHostTest/screenshots"
    }
    return "$directory/${scenario.name}-$screen.png"
  }

  private fun settle() {
    // Flush composition and platform drawing between frames, including lazy-item placement.
    repeat(3) {
      compose.mainClock.advanceTimeBy(1000)
      compose.waitForIdle()
    }
  }

  private fun capture(screen: String) {
    settle()
    compose.onNodeWithTag("screen").captureRoboImage(imagePath(screen))
  }

  private fun captureRoots(screen: String) {
    settle()
    compose.onAllNodes(isRoot()).fetchSemanticsNodes().indices.forEach { index ->
      compose.onAllNodes(isRoot())[index].captureRoboImage(imagePath("$screen-$index"))
    }
  }

  @Test fun loginFitsAndRetainsInputOnResize() {
    launch()
    compose.waitUntil(10_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 2 }
    compose.onAllNodes(hasSetTextAction())[0].performTextInput("reviewer")
    compose.onAllNodes(hasSetTextAction())[0].performKeyInput { pressKey(Key.Tab) }
    compose.onAllNodes(hasSetTextAction())[1].assertIsFocused()
    compose.runOnIdle { focusManager.clearFocus() }
    capture("login")
    if (scenario.fold?.startsWith("tabletop") == true) {
      val before = compose.onAllNodes(hasSetTextAction())[1].fetchSemanticsNode().boundsInWindow
      compose.runOnIdle { flat = !flat }
      settle()
      org.junit.Assert.assertEquals(before,
        compose.onAllNodes(hasSetTextAction())[1].fetchSemanticsNode().boundsInWindow)
      compose.onNodeWithText("Log in").performScrollTo().assertIsDisplayed()
    }
    compose.runOnIdle { width = if (width < 840) 900 else 400 }
    compose.onNodeWithText("reviewer").assertExists()
    compose.onNodeWithText("Log in").performScrollTo().assertIsDisplayed()
  }

  @Test fun mainDestinationsAndOverlays() {
    users.userFlow.value = UserModel("test", "Reviewer", "test-only", emailSearchHistory = listOf("Adaptive", "Window"))
    launch()
    waitFor("Adaptive review")
    capture("home")
    compose.onNodeWithText("Adaptive review").performClick()
    waitFor("Reply all")
    capture("detail")
    if (scenario.fold != null) {
      val before = compose.onNodeWithText("Reply all").fetchSemanticsNode().boundsInRoot
      compose.runOnIdle { flat = !flat }
      settle()
      org.junit.Assert.assertEquals(before, compose.onNodeWithText("Reply all").fetchSemanticsNode().boundsInRoot)
      compose.runOnIdle { flat = !flat }
    }
    compose.runOnIdle { width = if (width < 840) 900 else 400 }
    compose.onNodeWithText("Reply all").assertExists()
    compose.runOnIdle { width = scenario.width }
    compose.onNodeWithText("Favorites").performClick()
    waitFor("Adaptive review")
    capture("favorites")
    compose.onAllNodesWithText("Settings").onFirst().performClick()
    waitFor("Font size")
    capture("settings")
    compose.onNodeWithText("Home").performClick()
    waitFor("Adaptive review")
    compose.onNodeWithContentDescription("New email").performClick()
    waitFor("Recipients")
    // Dialog/Popup roots are separate from the page root and are captured explicitly.
    settle()
    compose.onNode(isDialog()).captureRoboImage(imagePath("draft"))
    compose.onNodeWithText("Cancel").performClick()
    compose.onNodeWithContentDescription("Search").performClick()
    waitFor("Adaptive")
    captureRoots("search-history")
    compose.onNodeWithText("Adaptive").performClick()
    waitFor("Adaptive review")
    captureRoots("search-results")
  }
}
