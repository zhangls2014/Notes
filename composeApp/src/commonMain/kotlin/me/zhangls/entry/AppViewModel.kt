package me.zhangls.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.framework.toast.ToastGlobalNotifier
import notes.composeapp.generated.resources.Res
import notes.composeapp.generated.resources.unlock_error_device_credential
import notes.composeapp.generated.resources.unlock_error_storage
import notes.composeapp.generated.resources.unlock_error_unavailable
import org.koin.core.annotation.KoinViewModel

/**
 * @author zhangls
 */
@KoinViewModel
class AppViewModel(
  savedStateHandle: SavedStateHandle,
  private val toastGlobalNotifier: ToastGlobalNotifier,
  private val userRepository: UserRepository,
  private val settingsRepository: SettingsRepository,
) : MviViewModel<AppState, AppIntent>(
  initialState = AppState(null),
  stateSerializer = AppState.serializer(),
  savedStateHandle = savedStateHandle,
  // 缓存主题等 UI 快照。会话确认和设备授权必须在本进程重新取得。
  savedKey = "state",
) {
  /**
   * 全局 Toast 队列
   */
  val toast = toastGlobalNotifier.toast
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

  private var sessionGeneration = 0L
  private var sessionUserId: String? = null

  internal fun onLogout() {
    sessionGeneration++
    sessionUserId = null
    updateState { copy(isLogin = false, biometricUnlockEnabled = false, deviceVerified = false) }
  }

  internal suspend fun verifyDevice(
    authenticator: DeviceAuthenticator,
    enabled: Boolean? = null,
    isCurrent: () -> Boolean = { true },
  ) {
    val snapshot = state.value
    val canVerify = if (enabled == null) {
      snapshot.access == AppAccess.Locked
    } else {
      snapshot.access == AppAccess.Ready && enabled != snapshot.biometricUnlockEnabled
    }
    if (!canVerify || snapshot.verifyingDevice || !isCurrent()) return

    val generation = sessionGeneration
    updateState { copy(verifyingDevice = true) }
    try {
      val result = authenticator.authenticate()
      currentCoroutineContext().ensureActive()
      if (generation != sessionGeneration || !isCurrent()) return
      when (result) {
        DeviceAuthenticationResult.Success -> {
          // Mark this process verified before publishing an enabled preference to the session flow.
          updateState { copy(deviceVerified = true) }
          if (enabled != null && !userRepository.updateBiometricUnlock(enabled)) {
            toastGlobalNotifier.showToast(Res.string.unlock_error_storage)
          }
        }
        DeviceAuthenticationResult.Cancelled -> Unit
        DeviceAuthenticationResult.Unavailable -> toastGlobalNotifier.showToast(Res.string.unlock_error_unavailable)
        DeviceAuthenticationResult.NoDeviceCredential ->
          toastGlobalNotifier.showToast(Res.string.unlock_error_device_credential)
      }
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      toastGlobalNotifier.showToast(Res.string.unlock_error_storage)
    } finally {
      updateState { copy(verifyingDevice = false) }
    }
  }

  override fun handleIntent(intent: AppIntent) {
    when (intent) {
      is AppIntent.DismissReleaseNotes -> viewModelScope.launch {
        settingsRepository.updateLastReadReleaseVersion(intent.version)
      }
    }
  }

  init {
    viewModelScope.launch {
      userRepository.userFlow.collectLatest { user ->
        val sessionChanged = sessionUserId != user?.id
        if (sessionChanged) sessionGeneration++
        sessionUserId = user?.id
        updateState {
          copy(
            isLogin = user != null,
            sessionLoaded = true,
            biometricUnlockEnabled = user?.biometricUnlockEnabled == true,
            deviceVerified = deviceVerified && !sessionChanged,
          )
        }
      }
    }
    viewModelScope.launch {
      settingsRepository.settingsFlow.collectLatest {
        updateState {
          copy(
            settingsLoaded = true,
            lastReadReleaseVersion = it.lastReadReleaseVersion,
            dynamicColor = it.dynamicColor,
            darkTheme = it.darkTheme,
            fontSize = it.fontSize,
            appLanguage = it.appLanguage,
          )
        }
      }
    }
  }
}
