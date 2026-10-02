package me.zhangls.entry.adaptive

import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.window.core.layout.WindowSizeClass
import me.zhangls.framework.toast.ToastGlobalNotifier
import me.zhangls.login.LoginScreen
import me.zhangls.login.mvi.LoginViewModel
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h500dp-mdpi")
class LoginLoadingAccessibilityTest {
  @get:Rule val compose = createComposeRule()
  private val store = ViewModelStore()

  @After fun clearViewModel() { compose.runOnIdle { store.clear() } }

  private fun launch(users: PendingLoginUsers, systemScale: Float = 1f, appScale: Float = 1f) {
    compose.setContent {
      val viewModel = remember {
        LoginViewModel(SavedStateHandle(), users, AdaptiveSettings(), ToastGlobalNotifier())
          .also { store.put("login", it) }
      }
      ProvideWindowAdaptiveInfo(WindowSizeClass(400, 500)) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, systemScale)) {
          ComposeAppTheme(fontScale = appScale) { LoginScreen(viewModel = viewModel, onLoginResult = {}) }
        }
      }
    }
  }

  @Test fun combinedSystemAndAppLargeFontsKeepLoginReachable() {
    launch(PendingLoginUsers(), systemScale = 2f, appScale = 2f)
    compose.onNodeWithText("Account").performScrollTo().assertExists()
    compose.onNodeWithText("Password").performScrollTo().assertExists()
    compose.onNodeWithText("Log in").performScrollTo().assertIsDisplayed()
  }

  @Test fun loadingDisablesFormActionsAndFailureRestoresThem() {
    val users = PendingLoginUsers()
    launch(users)
    compose.onAllNodes(hasSetTextAction())[0].performTextInput("reviewer")
    compose.onAllNodes(hasSetTextAction())[1].performTextInput("Test1234")
    compose.onNodeWithText("Password").performClick().assertIsFocused()
    compose.onNodeWithText("Log in").performScrollTo().performClick()
    compose.onNodeWithText("Log in").assertIsNotEnabled()
    compose.onNodeWithText("Account").assertIsNotEnabled()
    compose.onNodeWithText("Password").assertIsNotEnabled()
    // disabled TextField 不再提供 Focused 属性；缺省和 false 都表示没有输入焦点。
    for (label in listOf("Account", "Password")) {
      val config = compose.onNodeWithText(label).fetchSemanticsNode().config
      assertFalse(SemanticsProperties.Focused in config && config[SemanticsProperties.Focused])
    }
    compose.onNodeWithContentDescription("Clear account").assertIsNotEnabled()
    compose.onNodeWithContentDescription("Show password").assertIsNotEnabled()
    val loading = compose.onNodeWithContentDescription("Logging in").fetchSemanticsNode()
    assertEquals(LiveRegionMode.Polite, loading.config[SemanticsProperties.LiveRegion])
    compose.runOnIdle {
      assertEquals(1, users.loginCalls)
      users.result.completeExceptionally(IllegalStateException("Storage unavailable"))
    }
    compose.onNodeWithText("Log in").assertIsEnabled()
    compose.onNodeWithText("Account").assertIsEnabled()
    compose.onNodeWithText("Password").assertIsEnabled()
    compose.onNodeWithContentDescription("Clear account").assertIsEnabled()
    compose.onNodeWithContentDescription("Show password").assertIsEnabled()
    compose.onNodeWithContentDescription("Logging in").assertDoesNotExist()
  }
}
