package me.zhangls.login.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.zhangls.data.model.UserModel
import me.zhangls.data.model.AuthTokens
import kotlinx.coroutines.CancellationException
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.framework.toast.ToastGlobalNotifier
import me.zhangls.login.api.LoginResult
import me.zhangls.login.domain.LoginValidator
import notes.feature.login.generated.resources.Res
import notes.feature.login.generated.resources.login_msg_login_success
import notes.feature.login.generated.resources.login_msg_storage_failed
import org.koin.core.annotation.KoinViewModel

/**
 * @author zhangls
 */
@KoinViewModel
class LoginViewModel(
  savedStateHandle: SavedStateHandle,
  private val userRepository: UserRepository,
  private val settingsRepository: SettingsRepository,
  private val toastGlobalNotifier: ToastGlobalNotifier
) : MviViewModel<LoginState, LoginIntent>(
  initialState = LoginState(),
  stateSerializer = LoginState.serializer(),
  savedStateHandle = savedStateHandle
) {
  init {
    viewModelScope.launch {
      settingsRepository.settingsFlow.collectLatest {
        dispatch(LoginAction.UpdateLanguage(it.appLanguage))
        dispatch(LoginAction.UpdateDarkTheme(it.darkTheme))
      }
    }
  }

  override fun handleIntent(intent: LoginIntent) {
    // UI 的 disabled 不能替代业务守卫；加载期间也拒绝已排队的表单 Intent。
    if (state.value.isLoading && intent !is LoginIntent.UpdateLanguage && intent !is LoginIntent.UpdateDarkTheme) {
      return
    }
    when (intent) {
      LoginIntent.Login -> {
        if (LoginValidator.validateAll(state.value.account, state.value.password).not()) {
          dispatch(LoginAction.ValidationResult)
          return
        }
        // 必须在启动协程之前更新，后续排队的登录 Intent 才能看到加载状态。
        dispatch(LoginAction.Loading(true))
        login()
      }

      is LoginIntent.UpdateAccount -> {
        dispatch(LoginAction.UpdateAccount(intent.account))
      }

      is LoginIntent.ClearAccount -> {
        dispatch(LoginAction.ClearAccount)
      }

      is LoginIntent.UpdatePassword -> {
        dispatch(LoginAction.UpdatePassword(intent.password))
      }

      is LoginIntent.UpdatePasswordVisible -> {
        dispatch(LoginAction.UpdatePasswordVisible(intent.visible))
      }

      is LoginIntent.UpdateLanguage -> {
        dispatch(LoginAction.UpdateLanguage(intent.language))
        viewModelScope.launch {
          settingsRepository.updateAppLanguage(intent.language)
        }
      }

      is LoginIntent.UpdateDarkTheme -> {
        dispatch(LoginAction.UpdateDarkTheme(intent.config))
        viewModelScope.launch {
          settingsRepository.updateDarkTheme(intent.config)
        }
      }
    }
  }

  private fun dispatch(action: LoginAction) {
    updateState {
      LoginReducer.reduce(this, action)
    }
  }

  /**
   * 无后端环境下的本地模拟登录：直接构造用户并持久化。
   *
   * TODO: 接入真实登录接口后替换为网络请求
   */
  private fun login() {
    viewModelScope.launch {
      val user = withState {
        UserModel(
          id = MOCK_USER_ID,
          nickname = account,
          avatar = null,
        )
      }
      try {
        userRepository.login(user, AuthTokens(MOCK_ACCESS_TOKEN))
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        toastGlobalNotifier.showToast(Res.string.login_msg_storage_failed)
        return@launch
      } finally {
        dispatch(LoginAction.Loading(false))
      }
      toastGlobalNotifier.showToast(Res.string.login_msg_login_success)
      sendEffect(LoginResult.Success)
    }
  }

  companion object {
    private const val MOCK_USER_ID = "local"

    /** 本地模拟令牌，接入真实登录接口后移除 */
    private const val MOCK_ACCESS_TOKEN = "mock-access-token"
  }
}
