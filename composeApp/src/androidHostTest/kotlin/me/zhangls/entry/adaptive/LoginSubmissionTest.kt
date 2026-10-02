package me.zhangls.entry.adaptive

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
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
