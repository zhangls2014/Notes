package me.zhangls.login

import me.zhangls.framework.mvi.MviEffect

/**
 * @author zhangls
 */
sealed interface LoginResult : MviEffect {
  data object Success : LoginResult
  data class Error(val message: String) : LoginResult
  data object Cancel : LoginResult
}