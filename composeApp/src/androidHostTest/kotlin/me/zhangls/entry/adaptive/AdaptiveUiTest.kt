package me.zhangls.entry.adaptive

import androidx.activity.BackEventCompat
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.junit4.StateRestorationTester
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
      add(arrayOf(Scenario("landscape-large-font", 610, 320, fontScale = 2f)))
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
  private var height by mutableStateOf(scenario.height)
  private var flat by mutableStateOf(scenario.fold?.endsWith("flat") == true)
  private var verticalHingeCenter: Float? = null
  private lateinit var focusManager: androidx.compose.ui.focus.FocusManager
  private lateinit var inputModeManager: InputModeManager
  private lateinit var composeView: android.view.View

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

  private fun launch(restoration: StateRestorationTester? = null) {
    val content: @androidx.compose.runtime.Composable () -> Unit = {
      composeView = androidx.compose.ui.platform.LocalView.current
      inputModeManager = androidx.compose.ui.platform.LocalInputModeManager.current
      focusManager = androidx.compose.ui.platform.LocalFocusManager.current
      Box(Modifier.fillMaxSize()) {
        Box(Modifier.requiredSize(width.dp, height.dp).testTag("screen")) {
          ProvideWindowAdaptiveInfo(
            WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(width, height),
            posture(),
          ) {
            ComposeAppTheme(darkTheme = scenario.dark, fontScale = scenario.fontScale) {
              AppNavHost(viewModel = org.koin.compose.viewmodel.koinViewModel<AppViewModel>())
            }
          }
        }
      }
    }
    if (restoration == null) compose.setContent(content) else restoration.setContent(content)
  }

  private fun posture(): Posture {
    val fold = scenario.fold ?: return Posture()
    val vertical = fold.startsWith("book")
    val center = verticalHingeCenter ?: (width / 2f)
    val bounds = if (vertical) Rect(center - 5, 0f, center + 5, scenario.height.toFloat())
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

  @Test fun loginInputActionsAreAccessible() {
    org.junit.Assume.assumeTrue(scenario.name in listOf("400x500", "large-font", "dark"))
    launch()
    compose.waitUntil(10_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 2 }
    compose.onAllNodes(hasSetTextAction())[0].performTextInput("reviewer")
    compose.onNodeWithContentDescription("Clear account").assertHasClickAction().performClick()
    org.junit.Assert.assertEquals("", compose.onAllNodes(hasSetTextAction())[0].fetchSemanticsNode()
      .config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text)
    compose.onAllNodes(hasSetTextAction())[1].performTextInput("Test1234")
    compose.onNodeWithContentDescription("Show password").performClick()
    compose.onNodeWithContentDescription("Hide password").assertHasClickAction()
    compose.onNodeWithText("Test1234").assertExists()
    capture("login-input-visible")
    compose.onNodeWithContentDescription("Hide password").performClick()
    compose.onNodeWithContentDescription("Show password").assertExists()
    compose.onNodeWithText("Account").assertExists()
    compose.onNodeWithText("Password").assertExists()
    compose.onAllNodes(hasSetTextAction())[0].performTextInput("reviewer")
    compose.onAllNodes(hasSetTextAction())[1].performTextReplacement("short")
    compose.onNodeWithText("Password must be at least 8 characters").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Log in").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
    compose.runOnIdle { focusManager.clearFocus() }
    capture("login-input-error")
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

  @Test fun loginScrollViewportShrinksAboveIme() {
    org.junit.Assume.assumeTrue(scenario.name == "400x500")
    launch()
    compose.waitUntil(10_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 2 }
    val scroll = compose.onNode(hasScrollAction())
    val before = scroll.fetchSemanticsNode().boundsInWindow
    compose.runOnIdle {
      val insets = androidx.core.view.WindowInsetsCompat.Builder()
        .setInsets(androidx.core.view.WindowInsetsCompat.Type.ime(), androidx.core.graphics.Insets.of(0, 0, 0, 220))
        .setVisible(androidx.core.view.WindowInsetsCompat.Type.ime(), true)
        .build()
      androidx.core.view.ViewCompat.dispatchApplyWindowInsets(composeView, insets)
    }
    settle()
    val after = scroll.fetchSemanticsNode().boundsInWindow
    org.junit.Assert.assertTrue("IME must reduce the form's scroll viewport: $before -> $after", after.height < before.height)
    compose.onAllNodes(hasSetTextAction())[1].performClick()
    compose.onAllNodes(hasSetTextAction())[1].performScrollTo()
    compose.onAllNodes(hasSetTextAction())[1].assertIsDisplayed()
  }

  @Test fun loginImeSwitchKeepsViewportThroughKeyboardGap() {
    org.junit.Assume.assumeTrue(scenario.name == "400x500")
    launch()
    compose.waitUntil(10_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 2 }
    val scroll = compose.onNode(hasScrollAction())
    val withoutIme = scroll.fetchSemanticsNode().boundsInWindow.height
    fun ime(height: Int) = dispatchLoginIme(height)
    compose.onAllNodes(hasSetTextAction())[0].performClick()
    ime(220)
    settle()
    val withIme = scroll.fetchSemanticsNode().boundsInWindow.height
    org.junit.Assert.assertTrue(withIme < withoutIme)
    compose.mainClock.autoAdvance = false
    try {
      compose.onAllNodes(hasSetTextAction())[1].performSemanticsAction(SemanticsActions.RequestFocus)
      ime(0)
      repeat(6) { frame ->
        compose.mainClock.advanceTimeByFrame()
        org.junit.Assert.assertEquals("Keyboard handoff must not expand the viewport at frame $frame", withIme,
          scroll.fetchSemanticsNode().boundsInWindow.height, 0.5f)
      }
      ime(260)
      org.junit.Assert.assertTrue("Taller replacement keyboard must be avoided immediately",
        scroll.fetchSemanticsNode().boundsInWindow.height < withIme)
      ime(0)
      compose.mainClock.advanceTimeBy(300)
      org.junit.Assert.assertEquals("Real dismissal must release the inset even with input focus", withoutIme,
        scroll.fetchSemanticsNode().boundsInWindow.height, 0.5f)
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  @Test fun loginImeSwitchReleasesSmallerKeyboardAndCancelsStaleRelease() {
    org.junit.Assume.assumeTrue(scenario.name == "400x500")
    launch()
    compose.waitUntil(10_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 2 }
    fun ime(height: Int) = dispatchLoginIme(height, navigationBarHeight = 24)
    val scroll = compose.onNode(hasScrollAction())
    ime(0)
    val withoutIme = scroll.fetchSemanticsNode().boundsInWindow.height
    compose.onAllNodes(hasSetTextAction())[1].performSemanticsAction(SemanticsActions.RequestFocus)
    ime(260)
    settle()
    val withIme = scroll.fetchSemanticsNode().boundsInWindow.height
    org.junit.Assert.assertEquals("System bar space must not be counted twice", 236f,
      withoutIme - withIme, 0.5f)
    compose.mainClock.autoAdvance = false
    try {
      compose.onAllNodes(hasSetTextAction())[0].performSemanticsAction(SemanticsActions.RequestFocus)
      ime(0)
      compose.mainClock.advanceTimeBy(100)
      ime(180)
      compose.mainClock.advanceTimeBy(120)
      org.junit.Assert.assertEquals("New smaller keyboard cancels the earlier zero-height release", withIme,
        scroll.fetchSemanticsNode().boundsInWindow.height, 0.5f)
      compose.mainClock.advanceTimeBy(150)
      org.junit.Assert.assertEquals("Stable smaller keyboard releases excess space", withoutIme - 156f,
        scroll.fetchSemanticsNode().boundsInWindow.height, 0.5f)
      ime(0)
      compose.mainClock.advanceTimeBy(80)
      compose.onAllNodes(hasSetTextAction())[1].performSemanticsAction(SemanticsActions.RequestFocus)
      ime(280)
      compose.mainClock.advanceTimeBy(250)
      org.junit.Assert.assertEquals("Old release must not overwrite a taller replacement", withoutIme - 256f,
        scroll.fetchSemanticsNode().boundsInWindow.height, 0.5f)
      ime(0)
      compose.runOnIdle { focusManager.clearFocus() }
      compose.mainClock.advanceTimeByFrame()
      org.junit.Assert.assertEquals("Leaving the inputs releases protection immediately", withoutIme,
        scroll.fetchSemanticsNode().boundsInWindow.height, 0.5f)
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  @Test fun loginImeDismissalTracksInsetsWithoutDelay() {
    org.junit.Assume.assumeTrue(scenario.name == "400x500")
    launch()
    compose.waitUntil(10_000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 2 }
    val scroll = compose.onNode(hasScrollAction())
    val withoutIme = scroll.fetchSemanticsNode().boundsInWindow.height
    compose.onAllNodes(hasSetTextAction())[0].performSemanticsAction(SemanticsActions.RequestFocus)
    compose.mainClock.autoAdvance = false
    try {
      repeat(2) { round ->
        dispatchLoginIme(220)
        if (round == 1) {
          compose.onAllNodes(hasSetTextAction())[1].performSemanticsAction(SemanticsActions.RequestFocus)
          // A handoff with identical heights must also expire without another inset event.
          compose.mainClock.advanceTimeBy(500)
        }
        for (height in listOf(160, 80, 0)) {
          dispatchLoginIme(height)
          org.junit.Assert.assertEquals("Dismissal must follow each inset frame, round=$round height=$height",
            withoutIme - height, scroll.fetchSemanticsNode().boundsInWindow.height, 0.5f)
        }
      }
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  private fun dispatchLoginIme(height: Int, navigationBarHeight: Int = 0) {
    compose.runOnIdle {
      val insets = androidx.core.view.WindowInsetsCompat.Builder()
        .setInsets(androidx.core.view.WindowInsetsCompat.Type.ime(), androidx.core.graphics.Insets.of(0, 0, 0, height))
        .setVisible(androidx.core.view.WindowInsetsCompat.Type.ime(), height > 0)
        .setInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars(),
          androidx.core.graphics.Insets.of(0, 0, 0, navigationBarHeight))
        .setVisible(androidx.core.view.WindowInsetsCompat.Type.navigationBars(), navigationBarHeight > 0)
        .build()
      androidx.core.view.ViewCompat.dispatchApplyWindowInsets(composeView, insets)
    }
    compose.mainClock.advanceTimeByFrame()
    compose.waitForIdle()
  }

  @Test fun draftActionsRemainReachableAboveIme() {
    org.junit.Assume.assumeTrue(scenario.name == "400x500")
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    compose.onNodeWithContentDescription("New email").performClick()
    waitFor("Recipients")
    settle()
    val sheetScroll = compose.onAllNodes(hasScrollAction()).onLast()
    val before = sheetScroll.fetchSemanticsNode().boundsInWindow
    compose.runOnIdle {
      val dialog = checkNotNull(org.robolectric.shadows.ShadowDialog.getLatestDialog())
      val insets = androidx.core.view.WindowInsetsCompat.Builder()
        .setInsets(androidx.core.view.WindowInsetsCompat.Type.ime(), androidx.core.graphics.Insets.of(0, 0, 0, 220))
        .setVisible(androidx.core.view.WindowInsetsCompat.Type.ime(), true)
        .build()
      androidx.core.view.ViewCompat.dispatchApplyWindowInsets(dialog.window!!.decorView, insets)
    }
    settle()
    val after = sheetScroll.fetchSemanticsNode().boundsInWindow
    org.junit.Assert.assertTrue("IME must reduce the draft viewport: $before -> $after", after.height < before.height)
    compose.onNodeWithText("Save").performScrollTo().assertIsDisplayed()
  }

  @Test fun draftKeyboardHandoffKeepsViewportWithoutDelayingDismissal() {
    org.junit.Assume.assumeTrue(scenario.name == "400x500")
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    compose.onNodeWithContentDescription("New email").performClick()
    waitFor("Recipients")
    settle()
    val sheetScroll = compose.onAllNodes(hasScrollAction()).onLast()
    val withoutIme = sheetScroll.fetchSemanticsNode().boundsInWindow.height
    val dialog = checkNotNull(org.robolectric.shadows.ShadowDialog.getLatestDialog())
    fun ime(height: Int) {
      compose.runOnIdle {
        val insets = androidx.core.view.WindowInsetsCompat.Builder()
          .setInsets(androidx.core.view.WindowInsetsCompat.Type.ime(), androidx.core.graphics.Insets.of(0, 0, 0, height))
          .setVisible(androidx.core.view.WindowInsetsCompat.Type.ime(), height > 0)
          .build()
        androidx.core.view.ViewCompat.dispatchApplyWindowInsets(dialog.window!!.decorView, insets)
      }
      compose.mainClock.advanceTimeByFrame()
      compose.waitForIdle()
    }
    compose.onNodeWithText("Subject").performClick()
    ime(220)
    settle()
    val withIme = sheetScroll.fetchSemanticsNode().boundsInWindow.height
    org.junit.Assert.assertTrue(withIme < withoutIme)
    org.junit.Assert.assertEquals("Sheet must reserve the keyboard once", 216f, withIme, 0.5f)
    compose.mainClock.autoAdvance = false
    try {
      compose.onNodeWithText("Body").performClick()
      ime(0)
      repeat(6) { frame ->
        compose.mainClock.advanceTimeByFrame()
        org.junit.Assert.assertEquals("Draft handoff frame $frame", withIme,
          sheetScroll.fetchSemanticsNode().boundsInWindow.height, 0.5f)
      }
      ime(260)
      org.junit.Assert.assertTrue("Taller replacement keyboard must remain visible",
        sheetScroll.fetchSemanticsNode().boundsInWindow.height < withIme)
      val tallerViewport = sheetScroll.fetchSemanticsNode().boundsInWindow.height
      compose.mainClock.advanceTimeBy(500)
      ime(160)
      val smallerViewport = sheetScroll.fetchSemanticsNode().boundsInWindow.height
      org.junit.Assert.assertTrue("Normal dismissal grows the viewport on the first inset frame",
        smallerViewport > tallerViewport && smallerViewport < withoutIme)
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  @Test fun noHingeKeepsThePreferredListWidthWithTheDetailPlaceholder() {
    org.junit.Assume.assumeTrue(scenario.name == "900x400")
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    settle()
    val before = compose.onNodeWithText("Adaptive review").fetchSemanticsNode().boundsInRoot
    displayCutout(right = 62)
    val after = compose.onNodeWithText("Adaptive review").fetchSemanticsNode().boundsInRoot
    org.junit.Assert.assertEquals(360f - 32f, before.width, 1f)
    org.junit.Assert.assertEquals("The list keeps its preferred width inside the safe area", before, after)
  }

  @Test fun noHingeUsesPreferredWidthsInsideTheSafeAreaForEitherCutoutSide() {
    org.junit.Assume.assumeTrue(scenario.name == "900x400")
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    compose.onNodeWithText("Adaptive review").performClick()
    waitFor("Reply all")
    settle()
    fun paneBounds() = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes()
      .filter { it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange) }
      .map { it.boundsInRoot }.sortedBy { it.left }
    val withoutCutout = paneBounds()
    org.junit.Assert.assertEquals(2, withoutCutout.size)
    org.junit.Assert.assertTrue("The rail reserves horizontal space", withoutCutout[0].left > 0f)
    displayCutout(right = 62)
    val rightCutout = paneBounds()
    org.junit.Assert.assertEquals(2, rightCutout.size)
    org.junit.Assert.assertEquals(360f, rightCutout[0].width, 1f)
    org.junit.Assert.assertEquals(withoutCutout[1].width - 62f, rightCutout[1].width, 1f)
    org.junit.Assert.assertEquals(rightCutout[0].right, rightCutout[1].left, 1f)
    org.junit.Assert.assertEquals(838f, rightCutout[1].right, 1f)
    displayCutout(left = 62)
    val leftCutout = paneBounds()
    org.junit.Assert.assertEquals(withoutCutout[0].left + 62f, leftCutout[0].left, 1f)
    org.junit.Assert.assertEquals(360f, leftCutout[0].width, 1f)
    org.junit.Assert.assertEquals(rightCutout[1].width, leftCutout[1].width, 1f)
    org.junit.Assert.assertEquals(leftCutout[0].right, leftCutout[1].left, 1f)
    org.junit.Assert.assertEquals(900f, leftCutout[1].right, 1f)
  }

  @Test fun singlePaneStillAvoidsDisplayCutout() {
    org.junit.Assume.assumeTrue(scenario.name == "400x400")
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    settle()
    val before = compose.onNodeWithText("Adaptive review").fetchSemanticsNode().boundsInRoot
    displayCutout(right = 62)
    val after = compose.onNodeWithText("Adaptive review").fetchSemanticsNode().boundsInRoot
    org.junit.Assert.assertEquals(before.left, after.left, 0.5f)
    org.junit.Assert.assertEquals(before.right - 62, after.right, 0.5f)
  }

  @Test fun physicalHingeKeepsItsBoundaryWhenSystemInsetsChange() {
    org.junit.Assume.assumeTrue(scenario.name == "book-half")
    verticalHingeCenter = 320f
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    compose.onNodeWithText("Adaptive review").performClick()
    waitFor("Reply all")
    settle()
    fun paneBounds() = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes()
      .filter { it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange) }
      .map { it.boundsInRoot }.sortedBy { it.left }
    val before = paneBounds()
    org.junit.Assert.assertEquals(2, before.size)
    org.junit.Assert.assertEquals(315f, before[0].right, 1f)
    org.junit.Assert.assertEquals(325f, before[1].left, 1f)
    val replyBefore = compose.onNodeWithText("Reply all").fetchSemanticsNode().boundsInRoot
    displayCutout(right = 62)
    org.junit.Assert.assertEquals("A cutout must not move the physical pane split", before, paneBounds())
    org.junit.Assert.assertTrue(
      "The outside pane still avoids the cutout",
      compose.onNodeWithText("Reply all").fetchSemanticsNode().boundsInRoot.right < replyBefore.right,
    )
    compose.runOnIdle { flat = true }
    settle()
    org.junit.Assert.assertEquals("The same physical hinge stays fixed when unfolded", before, paneBounds())
  }

  private fun displayCutout(left: Int = 0, right: Int = 0) {
    compose.runOnIdle {
      val insets = androidx.core.view.WindowInsetsCompat.Builder()
        .setInsets(
          androidx.core.view.WindowInsetsCompat.Type.displayCutout(),
          androidx.core.graphics.Insets.of(left, 0, right, 0),
        )
        .build()
      androidx.core.view.ViewCompat.dispatchApplyWindowInsets(composeView, insets)
    }
    settle()
  }

  @Test fun returningFromDetailKeepsSearchCollapsed() {
    org.junit.Assume.assumeTrue(scenario.name == "400x1000")
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    settle()
    compose.onNodeWithContentDescription("Close search").assertDoesNotExist()
    compose.onNodeWithText("Adaptive review").performTouchInput { click() }
    waitFor("Reply all")
    settle()
    compose.runOnIdle {
      val dispatcher = checkNotNull(composeView.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
      dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 500f, 0f, BackEventCompat.EDGE_LEFT))
      dispatcher.dispatchOnBackProgressed(BackEventCompat(180f, 500f, 0.6f, BackEventCompat.EDGE_LEFT))
    }
    settle()
    compose.runOnIdle {
      checkNotNull(composeView.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher.onBackPressed()
    }
    waitFor("Adaptive review")
    settle()
    compose.onNodeWithContentDescription("Close search").assertDoesNotExist()
    compose.onNodeWithContentDescription("Search").performTouchInput { click() }
    compose.onAllNodesWithContentDescription("Close search").onLast().assertIsDisplayed()
    compose.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
  }

  @Test fun searchResultNavigationKeepsSearchClosedAfterReturnAndRotation() {
    org.junit.Assume.assumeTrue(scenario.name in listOf("400x1000", "610x400"))
    users.userFlow.value = UserModel("test", "Reviewer")
    val restoration = StateRestorationTester(compose)
    launch(restoration)
    waitFor("Adaptive review")
    settle()
    android.provider.Settings.Global.putFloat(
      org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    )
    compose.onNodeWithContentDescription("Search").performTouchInput { click() }
    settle()
    compose.onAllNodes(hasSetTextAction()).onLast().performTextInput("Adaptive")
    compose.waitUntil(10_000) { compose.onAllNodesWithText("Adaptive review").fetchSemanticsNodes().size == 2 }
    // The last matching row belongs to the search window, the first to the list behind it.
    compose.onAllNodesWithText("Adaptive review").onLast().performTouchInput { click() }
    waitFor("Reply all")
    settle()
    compose.onNodeWithContentDescription("Close search").assertDoesNotExist()
    compose.onNode(androidx.compose.ui.test.isPopup()).assertDoesNotExist()
    compose.runOnIdle {
      checkNotNull(composeView.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher.onBackPressed()
    }
    settle()
    compose.onNodeWithText("Reply all").assertDoesNotExist()
    compose.onNodeWithContentDescription("Close search").assertDoesNotExist()
    // Rotate the content window and recreate its saved composition, as Android does on rotation.
    compose.runOnIdle {
      width = scenario.height
      height = scenario.width
    }
    restoration.emulateSavedInstanceStateRestore()
    settle()
    compose.onNodeWithContentDescription("Close search").assertDoesNotExist()
    compose.onNode(androidx.compose.ui.test.isPopup()).assertDoesNotExist()
    compose.onNodeWithContentDescription("Search").performTouchInput { click() }
    settle()
    compose.onAllNodesWithContentDescription("Close search").onLast().assertIsDisplayed()
    compose.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
  }

  @Test fun landscapeSearchExpansionDoesNotJumpToFinalHeight() {
    org.junit.Assume.assumeTrue(scenario.name == "900x500")
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    settle()
    compose.mainClock.autoAdvance = false
    compose.onNodeWithContentDescription("Search").performTouchInput { click() }
    repeat(4) {
      compose.mainClock.advanceTimeByFrame()
      compose.waitForIdle()
    }
    val popup = compose.onNode(androidx.compose.ui.test.isPopup())
    val duringExpansion = popup.fetchSemanticsNode().boundsInWindow
    compose.mainClock.autoAdvance = true
    settle()
    val expanded = popup.fetchSemanticsNode().boundsInWindow
    org.junit.Assert.assertTrue(
      "Search must keep animating instead of resetting to its final height: $duringExpansion -> $expanded",
      duringExpansion.height < expanded.height,
    )
    org.junit.Assert.assertEquals(expanded.left, duringExpansion.left, 1f)
    org.junit.Assert.assertEquals(expanded.top, duringExpansion.top, 1f)
  }

  @Test fun longSearchQueryKeepsMeasuredWidth() {
    org.junit.Assume.assumeTrue(scenario.name in listOf("400x1000", "610x1000", "900x1000", "desktop"))
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    settle()
    val anchorBefore = compose.onNodeWithContentDescription("Search").fetchSemanticsNode().boundsInWindow
    if (java.lang.Boolean.getBoolean("notes.screenshots.candidates")) capture("measured-search-collapsed")
    compose.onNodeWithContentDescription("Search").performTouchInput { click() }
    settle()
    val input = compose.onAllNodes(hasSetTextAction()).onLast()
    val expandedBefore = input.fetchSemanticsNode().boundsInWindow
    for (query in listOf("Long search query 搜索内容 ".repeat(20), "short", "")) {
      input.performTextReplacement(query)
      settle()
      org.junit.Assert.assertEquals(expandedBefore, input.fetchSemanticsNode().boundsInWindow)
      if (query.length > 100 && java.lang.Boolean.getBoolean("notes.screenshots.candidates")) {
        captureRoots("measured-search-long-query")
      }
      input.assertIsFocused()
      org.junit.Assert.assertEquals(
        query, input.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text,
      )
      if (scenario.width >= 600) {
        val anchor = compose.onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInWindow
        org.junit.Assert.assertEquals(anchorBefore, anchor)
        org.junit.Assert.assertEquals(anchor.width, input.fetchSemanticsNode().boundsInWindow.width, 1f)
      }
    }
  }

  @Test fun restoredSearchUsesEmptyQueryMeasuredWidth() {
    org.junit.Assume.assumeTrue(scenario.name in listOf("610x1000", "desktop"))
    users.userFlow.value = UserModel("test", "Reviewer")
    val restoration = StateRestorationTester(compose)
    launch(restoration)
    waitFor("Adaptive review")
    settle()
    val initialWidth = compose.onNodeWithContentDescription("Search").fetchSemanticsNode().boundsInWindow.width
    compose.onNodeWithContentDescription("Search").performTouchInput { click() }
    settle()
    val query = "Restored long query 搜索 ".repeat(20)
    compose.onAllNodes(hasSetTextAction()).onLast().performTextInput(query)
    settle()
    restoration.emulateSavedInstanceStateRestore()
    settle()
    val input = compose.onAllNodes(hasSetTextAction()).onLast()
    org.junit.Assert.assertEquals(initialWidth, input.fetchSemanticsNode().boundsInWindow.width, 1f)
    org.junit.Assert.assertEquals(
      query, input.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text,
    )
    input.assertIsFocused()
  }

  @Test fun measuredSearchWidthShrinksAndRecoversWithWindow() {
    org.junit.Assume.assumeTrue(scenario.name == "900x500")
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    settle()
    val initialWidth = compose.onNodeWithContentDescription("Search").fetchSemanticsNode().boundsInWindow.width
    compose.onNodeWithContentDescription("Search").performTouchInput { click() }
    settle()
    val query = "Long resizing query ".repeat(20)
    compose.onAllNodes(hasSetTextAction()).onLast().performTextInput(query)
    for (newWidth in listOf(320, 900)) {
      compose.runOnIdle { width = newWidth }
      settle()
      val input = compose.onAllNodes(hasSetTextAction()).onLast()
      org.junit.Assert.assertEquals(
        query, input.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text,
      )
      input.assertIsFocused()
      if (newWidth == 900) {
        org.junit.Assert.assertEquals(initialWidth, input.fetchSemanticsNode().boundsInWindow.width, 1f)
      } else {
        // 测试只缩小页面约束，Dialog 仍使用 Robolectric 的实际宿主窗口尺寸。
        val anchorWidth = compose.onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInWindow.width
        org.junit.Assert.assertTrue("The anchor must obey the narrower page constraints", anchorWidth <= newWidth)
        org.junit.Assert.assertTrue(
          "Full-screen input must not be capped at the measured anchor width",
          input.fetchSemanticsNode().boundsInWindow.width > anchorWidth,
        )
      }
    }
  }

  @Test fun expandedSearchKeepsQueryWhenWindowWidthChanges() {
    org.junit.Assume.assumeTrue(scenario.name == "900x500")
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    compose.onNodeWithContentDescription("Search").performTouchInput { click() }
    settle()
    compose.onAllNodes(hasSetTextAction()).onLast().performTextInput("Adaptive")
    for (newWidth in listOf(400, 900)) {
      compose.runOnIdle { width = newWidth }
      settle()
      compose.onAllNodesWithContentDescription("Close search").onLast().assertIsDisplayed()
      org.junit.Assert.assertEquals(
        "Adaptive",
        compose.onAllNodes(hasSetTextAction()).onLast().fetchSemanticsNode()
          .config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text,
      )
    }
  }

  @Test fun searchCanReopenAfterCancellation() {
    org.junit.Assume.assumeTrue(scenario.name in listOf("400x1000", "900x1000"))
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    repeat(3) {
      compose.onNodeWithContentDescription("Search").performTouchInput { click() }
      settle()
      compose.onAllNodes(hasSetTextAction()).onLast().assertIsFocused()
      compose.onAllNodes(hasSetTextAction()).onLast().performTextInput("Adaptive")
      compose.onAllNodesWithContentDescription("Close search").onLast().performTouchInput { click() }
      settle()
      compose.onNodeWithContentDescription("Close search").assertDoesNotExist()
    }
  }

  @Test fun collapsedSearchRemainsKeyboardAccessible() {
    org.junit.Assume.assumeTrue(scenario.name == "900x1000")
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    compose.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
    compose.onNodeWithContentDescription("Search").performSemanticsAction(SemanticsActions.RequestFocus)
    compose.onNodeWithContentDescription("Search").assertIsFocused()
    compose.onNodeWithContentDescription("Close search").assertDoesNotExist()
    compose.onNodeWithContentDescription("Search").performKeyInput { pressKey(Key.DirectionDown) }
    settle()
    compose.onAllNodesWithContentDescription("Close search").onLast().assertIsDisplayed()
    compose.onAllNodes(hasSetTextAction()).onLast().performTextInput("Adaptive")
  }

  @Test fun compactHeightNavigationIsVerticalAndAllDestinationsRemainReachable() {
    org.junit.Assume.assumeTrue(scenario.width >= 600 && scenario.height < 480)
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    settle()
    val tabs = listOf("Home", "Favorites", "Settings").map {
      compose.onNode(hasText(it) and hasClickAction()).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
    }
    org.junit.Assert.assertTrue(tabs.zipWithNext().all { (a, b) -> a.bottom <= b.top })
    val screen = compose.onNodeWithTag("screen").fetchSemanticsNode().boundsInRoot
    org.junit.Assert.assertTrue("All labels fit fully inside the window", tabs.all {
      it.left >= screen.left && it.right <= screen.right &&
        it.top >= screen.top && it.bottom <= screen.bottom
    })
    org.junit.Assert.assertTrue(tabs.all { it.center.x < 150f })
    capture("navigation-rail")
    compose.onNodeWithText("Settings").performClick()
    waitFor("Font size")
    compose.onNode(hasText("Favorites") and hasClickAction()).performClick()
    waitFor("Adaptive review")
    compose.onNodeWithText("Home").performClick()
    waitFor("Adaptive review")
  }

  @Test fun detailPredictiveBackIncludesStatusBarSafeArea() {
    org.junit.Assume.assumeTrue(scenario.name in listOf("400x500", "400x1000"))
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    compose.runOnIdle {
      val insets = androidx.core.view.WindowInsetsCompat.Builder()
        .setInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars(),
          androidx.core.graphics.Insets.of(0, 64, 0, 0))
        .setVisible(androidx.core.view.WindowInsetsCompat.Type.statusBars(), true)
        .build()
      androidx.core.view.ViewCompat.dispatchApplyWindowInsets(composeView, insets)
    }
    settle()
    compose.onNodeWithText("Adaptive review").performClick()
    waitFor("Reply all")
    settle()
    val page = compose.onNodeWithTag("email-detail-page")
    val before = page.fetchSemanticsNode().boundsInRoot
    org.junit.Assert.assertEquals("Page background includes the status area", 0f, before.top, 0.5f)
    org.junit.Assert.assertTrue("Content still avoids status icons",
      compose.onAllNodesWithText("Adaptive review").fetchSemanticsNodes().all { it.boundsInRoot.top >= 64f })
    val dispatcher = checkNotNull(composeView.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
    compose.mainClock.autoAdvance = false
    try {
      compose.runOnIdle {
        dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 200f, 0f, BackEventCompat.EDGE_LEFT))
      }
      compose.mainClock.advanceTimeBy(100)
      compose.runOnIdle {
        dispatcher.dispatchOnBackProgressed(BackEventCompat(390f, 200f, 1f, BackEventCompat.EDGE_LEFT))
      }
      repeat(3) { compose.mainClock.advanceTimeBy(300); compose.waitForIdle() }
      val during = page.fetchSemanticsNode().boundsInRoot
      org.junit.Assert.assertEquals("Safe area moves with the centered page",
        (before.height - during.height) / 2f, during.top - before.top, 1f)
      org.junit.Assert.assertEquals("Page center X stays fixed", before.center.x, during.center.x, 0.5f)
      org.junit.Assert.assertEquals("Page center Y stays fixed", before.center.y, during.center.y, 0.5f)
      // Material child correction preserves the page content aspect ratio, including its safe area.
      org.junit.Assert.assertEquals("Scale includes safe-area height",
        before.height * (1f - 48f / before.width), during.height, 1f)
      compose.runOnIdle { dispatcher.dispatchOnBackCancelled() }
      repeat(3) { compose.mainClock.advanceTimeBy(500); compose.waitForIdle() }
      org.junit.Assert.assertEquals("Cancellation restores the complete page", before,
        page.fetchSemanticsNode().boundsInRoot)
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  @Test fun homePredictiveBackCommitKeepsFullyRevealedListInPlace() {
    assertPredictiveBackCommitKeepsListInPlace("Home")
  }

  @Test fun favoritesPredictiveBackCommitKeepsFullyRevealedListInPlace() {
    assertPredictiveBackCommitKeepsListInPlace("Favorites")
  }

  private fun assertPredictiveBackCommitKeepsListInPlace(tab: String) {
    // 单栏由 SinglePaneScene 接管预测返回；使用真实的 Entry、装饰器和弹栈流程。
    org.junit.Assume.assumeTrue(scenario.name == "400x500")
    android.provider.Settings.Global.putFloat(
      org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    )
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    compose.onNode(hasText(tab) and hasClickAction()).performClick()
    settle()
    val baseline = compose.onNodeWithText("Adaptive review").fetchSemanticsNode().boundsInRoot
    compose.onNodeWithText("Adaptive review").performClick()
    settle()
    val dispatcher = checkNotNull(composeView.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
    fun assertListInPlace(phase: String) {
      // 动画期间详情也可能保留同名节点；列表卡片必须始终位于完整显示时的位置。
      val bounds = compose.onAllNodesWithText("Adaptive review").fetchSemanticsNodes().map { it.boundsInRoot }
      org.junit.Assert.assertTrue("$tab $phase: expected $baseline in $bounds", baseline in bounds)
    }
    compose.mainClock.autoAdvance = false
    try {
      compose.runOnIdle {
        dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 200f, 0f, BackEventCompat.EDGE_LEFT))
      }
      compose.mainClock.advanceTimeBy(100)
      compose.runOnIdle {
        dispatcher.dispatchOnBackProgressed(BackEventCompat(390f, 200f, 1f, BackEventCompat.EDGE_LEFT))
      }
      compose.mainClock.advanceTimeBy(300)
      assertListInPlace("before release")
      compose.runOnIdle { dispatcher.onBackPressed() }
      // 覆盖提交后的连续帧，确认上一页始终保持已完全显示的位置。
      repeat(30) { frame ->
        compose.mainClock.advanceTimeByFrame()
        assertListInPlace("after release, frame ${frame + 1}")
      }
    } finally {
      compose.mainClock.autoAdvance = true
    }
    settle()
    compose.onNodeWithText("Adaptive review").assertIsDisplayed()
  }

  @Test fun landscapeDetailTransitionsStayOutsideNavigationRail() {
    org.junit.Assume.assumeTrue(scenario.name in listOf("610x400", "610x500"))
    android.provider.Settings.Global.putFloat(
      org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    )
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    settle()
    val baseline = compose.onNodeWithTag("screen").captureToImage().toPixelMap()
    // 取侧栏标签覆盖的横向范围，整段高度都不能被页面动画覆盖。
    val railRight = compose.onNode(hasText("Favorites") and hasClickAction()).fetchSemanticsNode().boundsInRoot.right.toInt()
    fun assertRailUnchanged(phase: String) {
      val frame = compose.onNodeWithTag("screen").captureToImage().toPixelMap()
      var changed = 0
      for (y in 0 until baseline.height) for (x in 0 until railRight) {
        if (baseline[x, y] != frame[x, y]) changed++
      }
      org.junit.Assert.assertEquals("$phase must not paint over the rail", 0, changed)
    }
    compose.mainClock.autoAdvance = false
    try {
      compose.onNodeWithText("Adaptive review").performClick()
      compose.mainClock.advanceTimeBy(80)
      assertRailUnchanged("enter")
      compose.mainClock.advanceTimeBy(1000)
      compose.runOnIdle {
        val dispatcher = checkNotNull(composeView.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
        dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 200f, 0f, BackEventCompat.EDGE_LEFT))
        dispatcher.dispatchOnBackProgressed(BackEventCompat(180f, 200f, 0.5f, BackEventCompat.EDGE_LEFT))
      }
      compose.mainClock.advanceTimeBy(80)
      assertRailUnchanged("predictive back")
      compose.runOnIdle {
        checkNotNull(composeView.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher.dispatchOnBackCancelled()
      }
      compose.mainClock.advanceTimeBy(1000)
      compose.runOnIdle {
        checkNotNull(composeView.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher.onBackPressed()
      }
      compose.mainClock.advanceTimeBy(80)
      assertRailUnchanged("pop")
    } finally {
      compose.mainClock.autoAdvance = true
    }
  }

  @Test fun tabStateRestoresInactiveStacksAndScrollAfterRecreation() {
    org.junit.Assume.assumeTrue(scenario.name == "400x1000")
    users.userFlow.value = UserModel("test", "Reviewer")
    val restoration = StateRestorationTester(compose)
    launch(restoration)
    waitFor("Adaptive review")
    compose.onAllNodes(hasScrollAction()).onFirst().performScrollToIndex(5)
    settle()
    val homeBounds = compose.onNodeWithText("Window layout 6").fetchSemanticsNode().boundsInRoot
    compose.onNode(hasText("Favorites") and hasClickAction()).performClick()
    waitFor("Adaptive review")
    compose.onNodeWithText("Adaptive review").performClick()
    waitFor("Reply all")
    compose.onNodeWithText("Settings").performClick()
    waitFor("Font size")
    restoration.emulateSavedInstanceStateRestore()
    settle()
    compose.onNodeWithText("Font size").assertIsDisplayed()
    compose.onNode(hasText("Favorites") and hasClickAction()).performClick()
    waitFor("Reply all")
    compose.onNodeWithText("Home").performClick()
    settle()
    waitFor("Window layout 6")
    org.junit.Assert.assertEquals(homeBounds, compose.onNodeWithText("Window layout 6").fetchSemanticsNode().boundsInRoot)
  }

  @Test fun tabStateRestoresScrollAndSelectionIndependently() {
    org.junit.Assume.assumeTrue(scenario.name in listOf("400x1000", "900x1000"))
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    compose.onAllNodes(hasScrollAction()).onFirst().performScrollToIndex(5)
    compose.onNodeWithText("Window layout 6").performTouchInput { longClick() }
    settle()
    compose.onNodeWithText("Window layout 6").assertIsSelected()
    val homeBounds = compose.onNodeWithText("Window layout 6").fetchSemanticsNode().boundsInRoot

    compose.onNode(hasText("Favorites") and hasClickAction()).performClick()
    waitFor("Adaptive review")
    compose.onNodeWithText("Adaptive review").assertIsNotSelected()
    compose.onAllNodes(hasScrollAction()).onFirst().performScrollToIndex(2)
    settle()
    val favoriteBounds = compose.onNodeWithText("Window layout 5").fetchSemanticsNode().boundsInRoot
    compose.onNodeWithText("Settings").performClick()
    waitFor("Font size")
    compose.onNodeWithText("Home").performClick()
    settle()
    compose.onNodeWithText("Window layout 6").assertIsSelected()
    org.junit.Assert.assertEquals(homeBounds, compose.onNodeWithText("Window layout 6").fetchSemanticsNode().boundsInRoot)
    compose.onNode(hasText("Favorites") and hasClickAction()).performClick()
    settle()
    compose.onNodeWithText("Window layout 5").assertIsNotSelected()
    org.junit.Assert.assertEquals(favoriteBounds, compose.onNodeWithText("Window layout 5").fetchSemanticsNode().boundsInRoot)
  }

  @Test fun tabSwitchPreservesNewEmailFabSize() {
    org.junit.Assume.assumeTrue(scenario.name == "400x1000")
    android.provider.Settings.Global.putFloat(
      org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    )
    users.userFlow.value = UserModel("test", "Reviewer")
    val restoration = StateRestorationTester(compose)
    launch(restoration)
    waitFor("Adaptive review")
    val fab = compose.onNodeWithContentDescription("New email")
    val expandedWidth = fab.fetchSemanticsNode().boundsInRoot.width
    compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
    settle()
    val collapsedWidth = fab.fetchSemanticsNode().boundsInRoot.width
    org.junit.Assert.assertTrue("Scrolling down collapses the FAB", collapsedWidth < expandedWidth)

    fun switchAwayAndBack(expectedWidth: Float) {
      compose.onNode(hasText("Settings") and hasClickAction()).performClick()
      waitFor("Font size")
      compose.mainClock.autoAdvance = false
      try {
        compose.onNode(hasText("Home") and hasClickAction()).performClick()
        // Inspect the restoration frames as well as the settled result.
        repeat(12) {
          compose.mainClock.advanceTimeByFrame()
          org.junit.Assert.assertEquals("Tab restoration must preserve FAB width", expectedWidth,
            fab.fetchSemanticsNode().boundsInRoot.width, 0.5f)
        }
      } finally {
        compose.mainClock.autoAdvance = true
      }
      settle()
      org.junit.Assert.assertEquals(expectedWidth, fab.fetchSemanticsNode().boundsInRoot.width, 0.5f)
    }

    switchAwayAndBack(collapsedWidth)
    compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput {
      swipeDown(startY = centerY, endY = centerY + 120f)
    }
    settle()
    org.junit.Assert.assertEquals("Scrolling toward the top expands the FAB", expandedWidth,
      fab.fetchSemanticsNode().boundsInRoot.width, 0.5f)
    switchAwayAndBack(expandedWidth)
    restoration.emulateSavedInstanceStateRestore()
    settle()
    org.junit.Assert.assertEquals("Recreation preserves the expanded FAB away from the top", expandedWidth,
      fab.fetchSemanticsNode().boundsInRoot.width, 0.5f)
    compose.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
    settle()
    org.junit.Assert.assertEquals("Restored FAB still responds to scrolling", collapsedWidth,
      fab.fetchSemanticsNode().boundsInRoot.width, 0.5f)
  }

  @Test fun tabStateRestoresEachOpenedDetail() {
    org.junit.Assume.assumeTrue(scenario.name in listOf("400x1000", "900x1000"))
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    compose.onNodeWithText("Adaptive review").performClick()
    waitFor("Reply all")
    compose.onNode(hasText("Favorites") and hasClickAction()).performClick()
    waitFor("Window layout 3")
    compose.onNodeWithText("Window layout 3").performClick()
    waitFor("Reply all")
    compose.onNodeWithText("Settings").performClick()
    waitFor("Font size")
    compose.onNodeWithText("Home").performClick()
    settle()
    compose.onNodeWithText("Reply all").assertIsDisplayed()
    compose.onAllNodesWithText("Adaptive review").onLast().assertIsDisplayed()
    compose.onNode(hasText("Favorites") and hasClickAction()).performClick()
    settle()
    compose.onNodeWithText("Reply all").assertIsDisplayed()
    compose.onAllNodesWithText("Window layout 3").onLast().assertIsDisplayed()
  }

  @Test fun tabRootsReleaseBackAndDetailsReturnToFavorites() {
    org.junit.Assume.assumeTrue(scenario.name in listOf("400x1000", "900x1000"))
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    for (tab in listOf("Favorites", "Settings")) {
      compose.onNode(hasText(tab) and hasClickAction()).performClick()
      settle()
      compose.runOnIdle {
        val dispatcher = checkNotNull(composeView.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher
        org.junit.Assert.assertFalse("$tab root must release system Back", dispatcher.hasEnabledCallbacks())
      }
    }
    compose.onNode(hasText("Favorites") and hasClickAction()).performClick()
    waitFor("Adaptive review")
    compose.onNodeWithText("Adaptive review").performClick()
    waitFor("Reply all")
    compose.onNode(hasText("Favorites") and hasClickAction()).performClick()
    compose.onNodeWithText("Reply all").assertExists()
    // 双栏已同时展示列表和详情；收窄后再验证页面级返回的归属。
    compose.runOnIdle { width = 400 }
    settle()
    compose.runOnIdle {
      checkNotNull(composeView.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher.onBackPressed()
    }
    settle()
    compose.onNodeWithText("Reply all").assertDoesNotExist()
    compose.onNodeWithText("Adaptive review").assertIsDisplayed()
    compose.onNode(hasText("Favorites") and hasClickAction()).assertIsSelected()
  }

  @Test fun tabSwitchDisposesAnInFlightDetailTransition() {
    org.junit.Assume.assumeTrue(scenario.name in listOf("400x1000", "900x1000"))
    android.provider.Settings.Global.putFloat(
      org.robolectric.RuntimeEnvironment.getApplication().contentResolver,
      android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    )
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    waitFor("Adaptive review")
    settle()
    compose.mainClock.autoAdvance = false
    try {
      compose.onNodeWithText("Adaptive review").performClick()
      compose.mainClock.advanceTimeByFrame()
      compose.onNodeWithText("Settings").performClick()
      compose.mainClock.advanceTimeByFrame()
      compose.waitForIdle()
      compose.onNodeWithText("Font size").assertExists()
      compose.onNodeWithText("Reply all").assertDoesNotExist()
      compose.onNodeWithText("Adaptive review").assertDoesNotExist()
      // 连续切换只展示目标 Tab；切回首页恢复详情，不恢复中途的动画。
      compose.onNode(hasText("Favorites") and hasClickAction()).performClick()
      compose.mainClock.advanceTimeByFrame()
      compose.onNodeWithText("Home").performClick()
      compose.mainClock.advanceTimeBy(1000)
      compose.onNodeWithText("Reply all").assertExists()
      compose.onNodeWithText("Font size").assertDoesNotExist()
    } finally {
      compose.mainClock.autoAdvance = true
    }
    waitFor("Reply all")
  }

  @Test fun settingsLogoutDialogKeepsCancelAndConfirmReachable() {
    org.junit.Assume.assumeTrue(scenario.name in setOf("400x500", "large-font", "dark"))
    users.userFlow.value = UserModel("test", "Reviewer")
    launch()
    compose.onAllNodesWithText("Settings").onFirst().performClick()
    waitFor("Font size")
    compose.onNode(hasScrollAction()).performScrollToNode(hasText("Log out"))
    compose.onNodeWithText("Log out").performScrollTo().assertIsDisplayed().performClick()
    waitFor("Confirm")
    compose.onNodeWithText("Cancel").assertIsDisplayed()
    compose.onAllNodesWithText("Log out").onLast().assertIsDisplayed()
    compose.onNode(isDialog()).captureRoboImage(imagePath("logout-dialog"))
    compose.onNodeWithText("Cancel").performClick()
    compose.onNodeWithText("Confirm").assertDoesNotExist()
    compose.onNodeWithText("Log out").performScrollTo().performClick()
    waitFor("Confirm")
    compose.onAllNodesWithText("Log out").onLast().performClick()
    waitFor("Log in")
    org.junit.Assert.assertEquals(2, compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size)
  }

  @Test fun mainDestinationsAndOverlays() {
    users.userFlow.value = UserModel("test", "Reviewer", emailSearchHistory = listOf("Adaptive", "Window"))
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
    compose.onNode(hasText("Favorites") and hasClickAction()).performClick()
    waitFor("Adaptive review")
    capture("favorites")
    compose.onAllNodesWithText("Settings").onFirst().performClick()
    waitFor("Font size")
    capture("settings")
    compose.onNodeWithText("Home").performClick()
    // 首页恢复刚才的详情；先在单栏返回列表，再继续验证列表浮层。
    compose.runOnIdle { width = 400 }
    settle()
    compose.runOnIdle {
      checkNotNull(composeView.findViewTreeOnBackPressedDispatcherOwner()).onBackPressedDispatcher.onBackPressed()
    }
    settle()
    compose.runOnIdle { width = scenario.width }
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
