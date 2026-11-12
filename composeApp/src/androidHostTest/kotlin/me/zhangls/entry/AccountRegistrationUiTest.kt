package me.zhangls.entry

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.login.api.AccountRegistration
import me.zhangls.login.api.LocalAccountRegistration
import me.zhangls.login.api.LoginDestination
import me.zhangls.login.api.LoginResult
import me.zhangls.login.api.RegistrationDestination
import me.zhangls.login.loginNavEntry
import me.zhangls.theme.ComposeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h800dp-mdpi")
class AccountRegistrationUiTest {
  @get:Rule val compose = createComposeRule()

  @Test fun verifiesEmailBeforeCreatingAccountAndPreservesRedirect() {
    val redirect = EmailDetailDestination(42)
    var completed: LoginDestination? = null
    val actions = mutableListOf<String>()
    val auth = object : AccountRegistration {
      override suspend fun sendSignupCode(email: String) {
        actions += "send:$email"
      }

      override suspend fun verifySignupCode(email: String, code: String): String {
        actions += "verify:$email:$code"
        return "proof"
      }

      override suspend fun registerAccount(email: String, proof: String, username: String, password: String) {
        check(password == "a long example password")
        actions += "create:$email:$proof:$username"
      }
    }
    val stack = NavBackStack<NavKey>(RegistrationDestination(redirect))
    compose.setContent {
      ComposeAppTheme {
        CompositionLocalProvider(LocalAccountRegistration provides auth) {
          NavDisplay(
            backStack = stack,
            onBack = {},
            entryProvider = entryProvider {
              loginNavEntry(onNavigate = {}) { result, destination ->
                assertEquals(LoginResult.Success, result)
                completed = destination
              }
            },
          )
        }
      }
    }

    compose.onNodeWithText("Verify email").assertIsDisplayed()
    compose.onNodeWithText("Continue").assertIsNotEnabled()
    enter("Email", "notes@example.com")
    compose.onNodeWithText("Send code").performClick()
    enter("Email verification code", "123456")
    compose.onNodeWithText("Continue").performClick()
    compose.onNodeWithText("Set up sign-in").assertIsDisplayed()
    enter("Username", "notes_tester")
    enter("Password", "a long example password")
    compose.onNodeWithText("Create account").performClick()
    compose.runOnIdle {
      assertEquals(
        listOf("send:notes@example.com", "verify:notes@example.com:123456", "create:notes@example.com:proof:notes_tester"),
        actions,
      )
      assertEquals(LoginDestination(redirect), completed)
    }
  }

  private fun enter(label: String, value: String) {
    compose.onNode(hasSetTextAction() and hasText(label)).performTextInput(value)
  }
}
