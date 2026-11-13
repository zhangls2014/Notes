package me.zhangls.entry

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.assertEquals
import me.zhangls.login.api.PasskeyAlreadyExistsException
import me.zhangls.login.api.PasskeyCreationTimedOutException
import me.zhangls.login.api.PasskeyCreationUnavailableException
import me.zhangls.theme.ComposeAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PasskeyManagementScreenTest {
  @get:Rule val compose = createComposeRule()

  @Test fun explainsWhyDuplicateCreationWasBlocked() {
    launch { throw PasskeyAlreadyExistsException() }
    addPasskey()
    compose.onNodeWithText(
      "The selected password manager already has a passkey for this Notes account.",
      substring = true,
    ).assertIsDisplayed()
    compose.onNodeWithText("No passkeys yet.").assertIsDisplayed()
  }

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
    compose.onNodeWithText("Could not add the passkey. Try again.").assertDoesNotExist()
  }

  @Test fun showsCreationMetadataAndConfirmsRevocation() {
    var passkeys = listOf(
      RegisteredPasskey("new", "2026-11-13T00:00:00Z", "google_password_manager", "Xiaomi"),
      RegisteredPasskey("old", "2026-11-12T00:00:00Z"),
    )
    var revoked: String? = null
    launch(listPasskeys = { passkeys }, revoke = { id ->
      revoked = id
      passkeys = passkeys.filterNot { it.id == id }
    }) { false }
    compose.onNodeWithText("Google Password Manager").assertIsDisplayed()
    compose.onNodeWithText("Creation device: Xiaomi").assertIsDisplayed()
    compose.onNodeWithText("Unknown password manager").assertIsDisplayed()
    compose.onNodeWithText("Creation device: Unknown").assertIsDisplayed()
    if (java.lang.Boolean.getBoolean("notes.screenshots.candidates")) {
      compose.onRoot().captureRoboImage("build/adaptive-candidates/passkey-management.png")
    }
    compose.onAllNodes(hasContentDescription("Passkey actions"))[0].performClick()
    compose.onNodeWithText("Revoke").performClick()
    compose.onNodeWithText("Revoke this passkey?").assertIsDisplayed()
    assertEquals(null, revoked)
    compose.onNodeWithText("Revoke").performClick()
    compose.onNodeWithText("Google Password Manager").assertDoesNotExist()
    assertEquals("new", revoked)
  }

  @Test fun compactScreenCentersTheListAndButtonAtTheSameWidth() {
    assertListAndButtonAlignment()
  }

  @Test
  @Config(qualifiers = "en-rUS-w1200dp-h900dp-mdpi")
  fun wideScreenCentersTheListAndButtonAtTheSameWidth() {
    assertListAndButtonAlignment()
  }

  private fun assertListAndButtonAlignment() {
    launch(listPasskeys = { listOf(RegisteredPasskey("id", "2026-11-13T00:00:00Z")) }) { false }
    val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
    val row = compose.onNodeWithTag("passkey-row").fetchSemanticsNode().boundsInRoot
    val more = compose.onNodeWithContentDescription("Passkey actions").fetchSemanticsNode().boundsInRoot
    val button = compose.onNodeWithText("Add passkey").fetchSemanticsNode().boundsInRoot
    assertEquals(root.center.x, row.center.x, 1f)
    assertEquals(root.center.x, button.center.x, 1f)
    assertEquals(button.left, row.left, 1f)
    assertEquals(button.right, row.right, 1f)
    assertEquals(row.center.y, more.center.y, 1f)
  }

  @Test fun retriesFailedListLoad() {
    var requests = 0
    launch(listPasskeys = {
      if (requests++ == 0) error("offline")
      emptyList()
    }) { false }
    compose.onNodeWithText("Could not load passkeys. Try again.").assertIsDisplayed()
    compose.onNodeWithText("Retry").performClick()
    compose.onNodeWithText("No passkeys yet.").assertIsDisplayed()
  }

  private fun launch(
    listPasskeys: suspend () -> List<RegisteredPasskey> = { emptyList() },
    revoke: suspend (String) -> Unit = {},
    add: suspend () -> Boolean,
  ) {
    compose.setContent {
      ComposeAppTheme {
        PasskeyManagementScreen(
          listPasskeys = listPasskeys,
          addPasskey = add,
          revokePasskey = revoke,
          onBack = {},
        )
      }
    }
  }

  private fun addPasskey() {
    val button = compose.onNodeWithText("Add passkey")
    button.performClick()
    button.assertIsEnabled()
  }
}
