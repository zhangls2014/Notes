package me.zhangls.login

import me.zhangls.framework.mvi.MviIntent

/**
 * @author zhangls
 */
sealed interface LoginIntent : MviIntent {
  object Login : LoginIntent
  data class UpdateAccount(val account: String) : LoginIntent
  object ClearAccount : LoginIntent
  data class UpdatePassword(val password: String) : LoginIntent
  data class UpdatePasswordVisible(val visible: Boolean) : LoginIntent
}