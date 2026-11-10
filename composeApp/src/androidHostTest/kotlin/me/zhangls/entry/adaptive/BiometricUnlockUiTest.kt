package me.zhangls.entry.adaptive

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.entry.App
import me.zhangls.entry.DeviceAuthenticationResult
import me.zhangls.entry.DeviceAuthenticator
import me.zhangls.entry.drainMainLooperAfter
import me.zhangls.entry.initKoin
import me.zhangls.entry.provideAboutAppInfo
import org.junit.After
import org.junit.Assert.assertEquals
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
@Config(sdk = [37], qualifiers = "en-rUS-w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BiometricUnlockUiTest {
  private val compose = createComposeRule()

  @get:Rule val rules = drainMainLooperAfter(compose)

  @After fun stop() = stopKoin()

  @Test fun lockHoldsDeepLinkAndReleaseNotesUntilSuccessfulAuthentication() {
    val users = AdaptiveUsers()
    users.userFlow.value = UserModel("local", "Notes", biometricUnlockEnabled = true)
    initKoin {
      modules(
        module {
          single<android.content.Context> { RuntimeEnvironment.getApplication() }
          single<UserRepository> { users }
          single<SettingsRepository> { AdaptiveSettings() }
          single<EmailsRepository> { AdaptiveEmails() }
        },
      )
    }
    provideAboutAppInfo(AboutAppInfo("1.1.0", "2"))
    val pending = mutableStateOf<String?>("https://notes.zhangls.me/email?id=3")
    var consumed = 0
    var missingCredential = true
    var response = DeviceAuthenticationResult.Cancelled
    val authenticator = object : DeviceAuthenticator {
      override fun isDeviceCredentialMissing(): Boolean = missingCredential

      override suspend fun authenticate(): DeviceAuthenticationResult = response
    }
    compose.setContent {
      App(
        deepLinkUrl = pending.value,
        onDeepLinkConsume = {
          consumed++
          pending.value = null
        },
        deviceAuthenticator = authenticator,
      )
    }
    compose.onNodeWithText("Notes is locked").assertIsDisplayed()
    val setupHint = compose.onNodeWithText("Set up a secure screen lock in system settings, then try again.")
    setupHint.assertIsDisplayed()
    compose.runOnIdle { missingCredential = false }
    compose.onNodeWithText("Unlock Notes").performClick()
    setupHint.assertDoesNotExist()
    compose.onNodeWithText("Notes is locked").assertIsDisplayed()
    compose.onNodeWithText("What’s new in 1.1.0").assertDoesNotExist()
    compose.onNodeWithText("Adaptive review 3").assertDoesNotExist()
    compose.runOnIdle {
      assertEquals(0, consumed)
      response = DeviceAuthenticationResult.Success
    }
    compose.onNodeWithText("Unlock Notes").performClick()
    compose.onNodeWithText("Notes is locked").assertDoesNotExist()
    compose.runOnIdle { assertEquals(1, consumed) }
  }
}
