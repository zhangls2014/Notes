package me.zhangls.login

import me.zhangls.framework.mvi.MviState

/**
 * @author zhangls
 */
data class LoginState(
  // 账户
  val account: String = "",
  // 密码
  val password: String = "",
  // 输入用户名错误
  val accountError: String? = null,
  // 输入密码错误
  val passwordError: String? = null,
  // 密码可见性
  val passwordVisible: Boolean = false,
  // 隐私声明是否勾选
  val privacyChecked: Boolean = false,
  // 输入账户密码格式是否正确
  val isInputValid: Boolean = false
) : MviState