package me.zhangls.login.domain

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.zhangls.login.R

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

@Composable
fun AccountError?.text(): String {
  return when (this) {
    is AccountError.Empty -> stringResource(id = R.string.login_hint_login_account)
    null -> ""
  }
}

/**
 * 密码错误
 */
sealed interface PasswordError : InputError {
  data object Empty : PasswordError
  data object TooShort : PasswordError
  data object WeakType : PasswordError
}

@Composable
fun PasswordError?.text(): String {
  return when (this) {
    is PasswordError.Empty -> stringResource(id = R.string.login_hint_login_password)
    is PasswordError.TooShort -> stringResource(id = R.string.login_hint_login_password_too_short)
    is PasswordError.WeakType -> stringResource(id = R.string.login_hint_login_password_weak_type)
    null -> ""
  }
}