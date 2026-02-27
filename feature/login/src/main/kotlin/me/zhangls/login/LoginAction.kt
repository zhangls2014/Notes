package me.zhangls.login

import me.zhangls.framework.mvi.MviAction

/**
 * @author zhangls
 */
sealed interface LoginAction : MviAction {
  data class UpdateAccount(val account: String) : LoginAction
  data object ClearAccount : LoginAction
  data class UpdatePassword(val password: String) : LoginAction
  data class UpdatePasswordVisible(val visible: Boolean) : LoginAction
  data object ValidationResult : LoginAction
  data class Loading(val visible: Boolean) : LoginAction
}