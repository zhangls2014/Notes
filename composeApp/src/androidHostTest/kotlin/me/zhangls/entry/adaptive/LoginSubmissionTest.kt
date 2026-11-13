package me.zhangls.entry.adaptive

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.zhangls.data.model.AuthTokens
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.toast.ToastGlobalNotifier
import me.zhangls.login.api.LoginResult
import me.zhangls.login.api.PasskeyLoginTimedOutException
import me.zhangls.login.api.PasskeyProvider
import me.zhangls.login.api.PasskeyResult
import me.zhangls.login.api.PasswordlessAuth
import me.zhangls.login.mvi.LoginIntent
import me.zhangls.login.mvi.LoginViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class LoginSubmissionTest {
  @Test fun queuedSubmissionsStartOnlyOneLoginAndFailureAllowsRetry() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val store = ViewModelStore()
    try {
      val users = PendingLoginUsers()
      val viewModel = LoginViewModel(SavedStateHandle(), users, AdaptiveSettings(), ToastGlobalNotifier())
      store.put("login", viewModel)
      runCurrent()
      viewModel.sendIntent(LoginIntent.UpdateAccount("reviewer"))
      viewModel.sendIntent(LoginIntent.UpdatePassword("Test1234"))
      runCurrent()
      repeat(3) { viewModel.sendIntent(LoginIntent.Login) }
      runCurrent()
      assertEquals(1, users.loginCalls)
      assertTrue(viewModel.state.value.isLoading)

      users.result.completeExceptionally(IllegalStateException("Storage unavailable"))
      runCurrent()
      assertFalse(viewModel.state.value.isLoading)
      users.result = CompletableDeferred()
      viewModel.sendIntent(LoginIntent.Login)
      runCurrent()
      assertEquals(2, users.loginCalls)
      users.result.complete(Unit)
      runCurrent()
      assertFalse(viewModel.state.value.isLoading)
    } finally {
      store.clear()
      Dispatchers.resetMain()
    }
  }

  @Test fun passkeyCancellationAndRejectedPasswordDoNotAuthenticate() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val store = ViewModelStore()
    try {
      val users = AdaptiveUsers()
      val viewModel = LoginViewModel(SavedStateHandle(), users, AdaptiveSettings(), ToastGlobalNotifier())
      val effects = mutableListOf<LoginResult>()
      var autofillCommits = 0
      val passkeyEmails = mutableListOf<String>()
      backgroundScope.launch { viewModel.effect.collect { if (it is LoginResult) effects.add(it) } }
      val provider = object : PasskeyProvider {
        override suspend fun create(optionsJson: String) = PasskeyResult.Cancelled
        override suspend fun get(optionsJson: String) = PasskeyResult.Cancelled
      }
      val auth = object : PasswordlessAuth {
        override suspend fun signInWithPassword(email: String, password: String, onPasswordVerified: (() -> Unit)?) {
          check(email == "Notes@example.com" && password == "an all letters passphrase")
          onPasswordVerified?.invoke()
        }
        override suspend fun signInWithPasskey(email: String, provider: PasskeyProvider): Boolean {
          passkeyEmails += email
          return provider.get("{}") is PasskeyResult.Success
        }
      }
      store.put("login", viewModel)
      runCurrent()
      viewModel.sendIntent(LoginIntent.SignInWithPasskey(auth, provider))
      runCurrent()
      assertTrue(effects.isEmpty())
      assertFalse(viewModel.state.value.isLoading)
      assertTrue(passkeyEmails.isEmpty())
      viewModel.sendIntent(LoginIntent.UpdateAccount(" Notes@example.com "))
      viewModel.sendIntent(LoginIntent.SignInWithPasskey(auth, provider))
      runCurrent()
      assertEquals(listOf("Notes@example.com"), passkeyEmails)
      assertTrue(effects.isEmpty())
      viewModel.sendIntent(LoginIntent.UpdatePassword("wrong"))
      viewModel.sendIntent(LoginIntent.PasswordLogin(auth) { autofillCommits++ })
      runCurrent()
      assertTrue(effects.isEmpty())
      assertEquals(0, autofillCommits)
      assertFalse(viewModel.state.value.isLoading)
      viewModel.sendIntent(LoginIntent.UpdatePassword("an all letters passphrase"))
      viewModel.sendIntent(LoginIntent.PasswordLogin(auth) { autofillCommits++ })
      runCurrent()
      assertEquals(listOf<LoginResult>(LoginResult.Success), effects)
      assertEquals(1, autofillCommits)
    } finally {
      store.clear()
      Dispatchers.resetMain()
    }
  }

  @Test fun loadingRejectsFormChangesAndClearsOnCancellation() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val store = ViewModelStore()
    try {
      val users = PendingLoginUsers()
      val viewModel = LoginViewModel(SavedStateHandle(), users, AdaptiveSettings(), ToastGlobalNotifier())
      store.put("login", viewModel)
      runCurrent()
      viewModel.sendIntent(LoginIntent.UpdateAccount("reviewer"))
      viewModel.sendIntent(LoginIntent.UpdatePassword("Test1234"))
      runCurrent()
      viewModel.sendIntent(LoginIntent.Login)
      runCurrent()
      val loadingState = viewModel.state.value
      viewModel.sendIntent(LoginIntent.ClearAccount)
      viewModel.sendIntent(LoginIntent.UpdateAccount("changed"))
      viewModel.sendIntent(LoginIntent.UpdatePassword("Changed123"))
      viewModel.sendIntent(LoginIntent.UpdatePasswordVisible(true))
      viewModel.sendIntent(LoginIntent.Login)
      runCurrent()
      assertEquals(loadingState, viewModel.state.value)
      assertEquals(1, users.loginCalls)
      users.result.cancel()
      runCurrent()
      assertFalse(viewModel.state.value.isLoading)
    } finally {
      store.clear()
      Dispatchers.resetMain()
    }
  }

  @Test fun passkeyTimeoutRestoresPasswordLoginAndShowsFeedback() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val store = ViewModelStore()
    try {
      val notifier = ToastGlobalNotifier()
      val messages = mutableListOf<String>()
      backgroundScope.launch { notifier.toast.collect { messages += it.resId.key } }
      val viewModel = LoginViewModel(SavedStateHandle(), AdaptiveUsers(), AdaptiveSettings(), notifier)
      store.put("login", viewModel)
      val result = CompletableDeferred<Boolean>()
      var passwordAttempts = 0
      val auth = object : PasswordlessAuth {
        override suspend fun signInWithPassword(email: String, password: String, onPasswordVerified: (() -> Unit)?) {
          passwordAttempts++
        }

        override suspend fun signInWithPasskey(email: String, provider: PasskeyProvider) = result.await()
      }
      val provider = object : PasskeyProvider {
        override suspend fun create(optionsJson: String) = PasskeyResult.Cancelled
        override suspend fun get(optionsJson: String) = PasskeyResult.Cancelled
      }
      runCurrent()
      viewModel.sendIntent(LoginIntent.UpdateAccount("notes@example.com"))
      viewModel.sendIntent(LoginIntent.UpdatePassword("Test1234"))
      viewModel.sendIntent(LoginIntent.SignInWithPasskey(auth, provider))
      runCurrent()
      assertTrue(viewModel.state.value.isLoading)

      result.completeExceptionally(PasskeyLoginTimedOutException())
      runCurrent()
      assertFalse(viewModel.state.value.isLoading)
      assertEquals(listOf("login_passkey_timeout"), messages)

      viewModel.sendIntent(LoginIntent.PasswordLogin(auth))
      runCurrent()
      assertEquals(1, passwordAttempts)
    } finally {
      store.clear()
      Dispatchers.resetMain()
    }
  }
}

internal class PendingLoginUsers : UserRepository by AdaptiveUsers() {
  var result = CompletableDeferred<Unit>()
  var loginCalls = 0
    private set

  override suspend fun login(user: UserModel, tokens: AuthTokens) {
    loginCalls++
    result.await()
  }
}
