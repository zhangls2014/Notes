package me.zhangls.entry

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.autofill.FillableData
import androidx.compose.ui.autofill.createFromText
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.CompletableDeferred
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.login.api.AccountRegistration
import me.zhangls.login.api.EmailAlreadyRegisteredException
import me.zhangls.login.api.LocalAccountRegistration
import me.zhangls.login.api.LoginDestination
import me.zhangls.login.api.LoginResult
import me.zhangls.login.api.RegistrationDestination
import me.zhangls.login.loginNavEntry
import me.zhangls.theme.ComposeAppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import me.zhangls.entry.createDiagnosedComposeRule as createComposeRule

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h800dp-mdpi")
class AccountRegistrationUiTest {
  @get:Rule val compose = createComposeRule()

  @Test fun retainsRegistrationAcrossRecompositionOfTheScreenAndPreservesRedirect() {
    val redirect = EmailDetailDestination(42)
    var completed: LoginDestination? = null
    val screenGeneration = mutableIntStateOf(0)
    val sendFinished = CompletableDeferred<Unit>()
    val actions = mutableListOf<String>()
    val auth = object : AccountRegistration {
      override suspend fun sendSignupCode(email: String) {
        actions += "send:$email"
        if (email == "registered@example.com") throw EmailAlreadyRegisteredException()
        sendFinished.await()
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
      DiagnosticAutofillTheme {
        CompositionLocalProvider(LocalAccountRegistration provides auth) {
          NavDisplay(
            backStack = stack,
            onBack = {},
            entryDecorators = listOf(
              rememberSaveableStateHolderNavEntryDecorator(),
              rememberViewModelStoreNavEntryDecorator(),
              remember {
                NavEntryDecorator<NavKey> { entry ->
                  key(screenGeneration.intValue) { entry.Content() }
                }
              },
            ),
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
    val emailBounds = compose.onNode(hasSetTextAction() and hasText("Email")).getUnclippedBoundsInRoot()
    val buttonBounds = compose.onNodeWithText("Send code").getUnclippedBoundsInRoot()
    // At mdpi and default font scale, the field reserves 8 dp above its outline for the label.
    val outlineCenter = (emailBounds.top.value + 8f + emailBounds.bottom.value) / 2
    val buttonCenter = (buttonBounds.top.value + buttonBounds.bottom.value) / 2
    assertEquals(outlineCenter, buttonCenter, 1f)
    autofillAndCheckHighlight("Email", "registered@example.com")
    compose.onNodeWithText("Send code").performClick()
    compose.onNodeWithText("This email is already registered. Please log in.").assertIsDisplayed()
    autofillAndCheckHighlight("Email verification code", "123456")
    compose.onNodeWithText("Continue").assertIsNotEnabled()
    compose.onNode(hasSetTextAction() and hasText("Email")).performTextReplacement("notes@example.com")
    compose.onNodeWithText("Send code").performClick()
    compose.runOnIdle { screenGeneration.intValue++ }
    compose.onNodeWithText("notes@example.com").assertIsDisplayed()
    compose.runOnIdle { sendFinished.complete(Unit) }
    enter("Email verification code", "123456")
    compose.runOnIdle { screenGeneration.intValue++ }
    compose.onNodeWithText("Resend in", substring = true).assertIsNotEnabled()
    compose.onNodeWithText("Continue").performClick()
    compose.onNodeWithText("Set up sign-in").assertIsDisplayed()
    enter("Username", "notes_tester")
    enter("Password", "a long example password")
    compose.runOnIdle { screenGeneration.intValue++ }
    compose.onNodeWithText("Create account").performClick()
    compose.runOnIdle {
      assertEquals(
        listOf(
          "send:registered@example.com",
          "send:notes@example.com",
          "verify:notes@example.com:123456",
          "create:notes@example.com:proof:notes_tester",
        ),
        actions,
      )
      assertEquals(LoginDestination(redirect), completed)
    }
  }

  private fun autofillAndCheckHighlight(label: String, value: String) {
    val field = compose.onNode(hasSetTextAction() and hasText(label))
    fun captureField(): androidx.compose.ui.graphics.PixelMap {
      val bounds = field.fetchSemanticsNode().boundsInRoot
      val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
      return android.graphics.Bitmap.createBitmap(
        bitmap,
        bounds.left.toInt(),
        bounds.top.toInt(),
        bounds.width.toInt(),
        bounds.height.toInt(),
      ).asImageBitmap().toPixelMap()
    }
    val before = captureField()
    field.performSemanticsAction(SemanticsActions.OnFillData) {
      assertTrue(it(checkNotNull(FillableData.createFromText(value))))
    }
    compose.mainClock.advanceTimeByFrame()
    compose.waitForIdle()
    val filled = captureField()
    assertEquals(before[1, filled.height - 2], filled[1, filled.height - 2])
    assertTrue(
      (0 until filled.height).any { y ->
        (0 until filled.width).any { x -> filled[x, y].let { it.red > 0.95f && it.blue > 0.95f && it.green < 0.05f } }
      },
    )
  }

  private fun enter(label: String, value: String) {
    compose.onNode(hasSetTextAction() and hasText(label)).performTextInput(value)
  }
}

@androidx.compose.runtime.Composable
private fun DiagnosticAutofillTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
  androidx.compose.runtime.CompositionLocalProvider(
    androidx.compose.foundation.text.LocalAutofillHighlightBrush provides
      androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Magenta),
  ) { ComposeAppTheme(content = content) }
}
