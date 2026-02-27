package me.zhangls.login.domain

/**
 * 统一的输入错误模型
 */
sealed interface InputError

/**
 * 账号错误
 */
sealed interface AccountError : InputError {
  data object Empty : AccountError
}

/**
 * 密码错误
 */
sealed interface PasswordError : InputError {
  data object Empty : PasswordError
  data object TooShort : PasswordError
  data object WeakType : PasswordError
}