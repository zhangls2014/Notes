package me.zhangls.login.domain

import androidx.compose.runtime.Composable
import notes.feature.login.generated.resources.Res
import notes.feature.login.generated.resources.login_hint_login_account
import notes.feature.login.generated.resources.login_hint_login_password
import notes.feature.login.generated.resources.login_hint_login_password_too_short
import notes.feature.login.generated.resources.login_hint_login_password_weak_type
import org.jetbrains.compose.resources.stringResource

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
    is AccountError.Empty -> stringResource(Res.string.login_hint_login_account)
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
    is PasswordError.Empty -> stringResource(Res.string.login_hint_login_password)
    is PasswordError.TooShort -> stringResource(Res.string.login_hint_login_password_too_short)
    is PasswordError.WeakType -> stringResource(Res.string.login_hint_login_password_weak_type)
    null -> ""
  }
}