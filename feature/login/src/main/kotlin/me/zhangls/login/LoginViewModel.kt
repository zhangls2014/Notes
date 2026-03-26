package me.zhangls.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.framework.mvi.ToastGlobalNotifier
import me.zhangls.login.domain.LoginValidator
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
      }
    }
  }

  override fun handleIntent(intent: LoginIntent) {
    when (intent) {
      LoginIntent.Login -> {
        if (LoginValidator.validateAll(state.value.account, state.value.password).not()) {
          dispatch(LoginAction.ValidationResult)
          return
        }

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
    }
  }

  private fun dispatch(action: LoginAction) {
    updateState {
      LoginReducer.reduce(this, action)
    }
  }

  /**
   * 模拟登录保存
   */
  private fun login() {
    viewModelScope.launch {
      dispatch(LoginAction.Loading(true))

      delay(1000)
      val user = withState {
        UserModel(id = (0..100).random().toString(), nickname = account, accessToken = "123456")
      }
      userRepository.update(user)

      dispatch(LoginAction.Loading(false))
      toastGlobalNotifier.showToast(resId = R.string.login_msg_login_success)
      sendEffect(LoginResult.Success)
    }
  }
}
