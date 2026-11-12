package me.zhangls.entry.adaptive

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.savedstate.serialization.encodeToSavedState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.UserRepository
import me.zhangls.entry.AppAccess
import me.zhangls.entry.AppViewModel
import me.zhangls.entry.DeviceAuthenticationResult
import me.zhangls.entry.configuredAuthEndpoint
import me.zhangls.framework.toast.ToastGlobalNotifier
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37])
class BiometricUnlockTest {
  @Test
  fun lockedSessionRequiresSuccessfulVerification() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val store = ViewModelStore()
    try {
      val users = AdaptiveUsers()
      users.userFlow.value = UserModel(
        "account-a",
        "Notes",
        authOrigin = configuredAuthEndpoint(),
        biometricUnlockEnabled = true,
      )
      val handle = SavedStateHandle()
      val viewModel = AppViewModel(handle, ToastGlobalNotifier(), users, AdaptiveSettings())
      store.put("app", viewModel)
      assertEquals(AppAccess.Loading, viewModel.state.value.access)
      runCurrent()
      assertEquals(AppAccess.Locked, viewModel.state.value.access)
      viewModel.verifyDevice({ DeviceAuthenticationResult.Cancelled })
      assertEquals(AppAccess.Locked, viewModel.state.value.access)
      viewModel.verifyDevice({ DeviceAuthenticationResult.Success })
      assertEquals(AppAccess.Ready, viewModel.state.value.access)
      runCurrent()
      store.clear()
      val restoredHandle = SavedStateHandle(
        mapOf("state" to encodeToSavedState(viewModel.state.value)),
      )
      val recreated = AppViewModel(restoredHandle, ToastGlobalNotifier(), users, AdaptiveSettings())
      store.put("recreated", recreated)
      assertEquals(true, recreated.state.value.isLogin)
      assertEquals(AppAccess.Loading, recreated.state.value.access)
      runCurrent()
      assertEquals(AppAccess.Locked, recreated.state.value.access)
    } finally {
      store.clear()
      Dispatchers.resetMain()
    }
  }

  @Test
  fun changingTheSettingRequiresVerificationAndSuccessfulStorage() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val store = ViewModelStore()
    try {
      val users = AdaptiveUsers()
      users.userFlow.value = UserModel("account-a", "Notes", authOrigin = configuredAuthEndpoint())
      var failWrites = true
      val repository = object : UserRepository by users {
        override suspend fun updateBiometricUnlock(enabled: Boolean): Boolean =
          if (failWrites) error("storage unavailable") else users.updateBiometricUnlock(enabled)
      }
      val viewModel = AppViewModel(SavedStateHandle(), ToastGlobalNotifier(), repository, AdaptiveSettings())
      store.put("app", viewModel)
      runCurrent()
      viewModel.verifyDevice({ DeviceAuthenticationResult.Success }, enabled = true)
      assertEquals(false, users.userFlow.value?.biometricUnlockEnabled)

      failWrites = false
      viewModel.verifyDevice({ DeviceAuthenticationResult.Cancelled }, enabled = true)
      assertEquals(false, users.userFlow.value?.biometricUnlockEnabled)
      viewModel.verifyDevice({ DeviceAuthenticationResult.Success }, enabled = true)
      runCurrent()
      assertEquals(true, users.userFlow.value?.biometricUnlockEnabled)
      assertEquals(AppAccess.Ready, viewModel.state.value.access)
      viewModel.verifyDevice({ DeviceAuthenticationResult.Cancelled }, enabled = false)
      assertEquals(true, users.userFlow.value?.biometricUnlockEnabled)
      viewModel.verifyDevice({ DeviceAuthenticationResult.Success }, enabled = false)
      assertEquals(false, users.userFlow.value?.biometricUnlockEnabled)
    } finally {
      store.clear()
      Dispatchers.resetMain()
    }
  }

  @Test
  fun lateVerificationCannotUnlockAnotherSessionAndConcurrentRequestsAreIgnored() = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    val store = ViewModelStore()
    try {
      val users = AdaptiveUsers()
      val user = UserModel("account-a", "Notes", authOrigin = configuredAuthEndpoint(), biometricUnlockEnabled = true)
      users.userFlow.value = user
      val handle = SavedStateHandle()
      val viewModel = AppViewModel(handle, ToastGlobalNotifier(), users, AdaptiveSettings())
      store.put("app", viewModel)
      runCurrent()
      val response = CompletableDeferred<DeviceAuthenticationResult>()
      val request = launch { viewModel.verifyDevice({ response.await() }) }
      runCurrent()
      viewModel.verifyDevice({ error("Second request reached the system prompt") })
      users.clear()
      runCurrent()
      users.userFlow.value = user
      runCurrent()
      response.complete(DeviceAuthenticationResult.Success)
      request.join()
      assertEquals(AppAccess.Locked, viewModel.state.value.access)
    } finally {
      store.clear()
      Dispatchers.resetMain()
    }
  }
}
