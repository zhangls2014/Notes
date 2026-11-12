package me.zhangls.entry

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import me.zhangls.login.api.PasskeyCreationTimedOutException
import me.zhangls.login.api.PasskeyCreationUnavailableException
import me.zhangls.theme.ComposeAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h800dp-mdpi")
class PasskeyManagementDialogTest {
  @get:Rule val compose = createComposeRule()

  @Test fun explainsWhenNoPasskeyProviderIsAvailable() {
    launch { throw PasskeyCreationUnavailableException() }
    addPasskey()
    compose.onNodeWithText("No passkey service is available.", substring = true).assertIsDisplayed()
  }

  @Test fun explainsWhenPasskeyCreationTimesOut() {
    launch { throw PasskeyCreationTimedOutException() }
    addPasskey()
    compose.onNodeWithText("The passkey service did not respond. Try again.").assertIsDisplayed()
  }

  @Test fun allowsRetryWithoutAnErrorWhenTheUserCancels() {
    launch { false }
    addPasskey()
    compose.onNodeWithText("No passkeys yet.").assertIsDisplayed()
    compose.onNodeWithText("No passkey service is available.", substring = true).assertDoesNotExist()
    compose.onNodeWithText("The passkey service did not respond. Try again.").assertDoesNotExist()
    compose.onNodeWithText("Could not manage passkeys. Try again.").assertDoesNotExist()
  }

  private fun launch(add: suspend () -> Boolean) {
    compose.setContent {
      ComposeAppTheme {
        PasskeyManagementDialog(
          listPasskeys = { emptyList() },
          addPasskey = add,
          revokePasskey = {},
          onDismiss = {},
        )
      }
    }
    compose.onNodeWithText("No passkeys yet.").assertIsDisplayed()
  }

  private fun addPasskey() {
    val button = compose.onNodeWithText("Add passkey")
    button.performClick()
    button.assertIsEnabled()
  }
}
