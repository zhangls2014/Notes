package me.zhangls.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.zhangls.data.model.UserModel
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.framework.mvi.ToastEffect
import javax.inject.Inject

/**
 * @author zhangls
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val userRepository: UserRepository
) : MviViewModel<LoginState, LoginIntent>(
  initialState = LoginState(),
  stateSerializer = LoginState.serializer(),
  savedStateHandle = savedStateHandle
) {
  override fun handleIntent(intent: LoginIntent) {
    when (intent) {
      LoginIntent.Login -> {
        viewModelScope.launch {
          val user = withState {
            UserModel(id = (0..100).random().toString(), nickname = account, accessToken = "123456")
          }
          userRepository.update(user)
        }
        sendEffect(ToastEffect("登录成功"))
        sendEffect(LoginResult.Success)
      }

      is LoginIntent.UpdateAccount -> {
        updateState {
          copy(
            account = intent.account,
            accountError = if (intent.account.length < 6) "账户长度不能小于6位" else null,
            isInputValid = intent.account.length >= 6 && state.value.password.length >= 6
          )
        }
      }

      is LoginIntent.ClearAccount -> {
        updateState { copy(account = "", accountError = null, isInputValid = false) }
      }

      is LoginIntent.UpdatePassword -> {
        updateState {
          copy(
            password = intent.password,
            passwordError = if (intent.password.length < 6) "密码长度不能小于6位" else null,
            isInputValid = intent.password.length >= 6 && state.value.account.length >= 6
          )
        }
      }

      is LoginIntent.UpdatePasswordVisible -> {
        updateState { copy(passwordVisible = intent.visible) }
      }
    }
  }
}

