package me.zhangls.login.domain

import androidx.compose.runtime.Composable
import notes.feature.login.generated.resources.Res
import notes.feature.login.generated.resources.login_hint_login_account
import notes.feature.login.generated.resources.login_hint_login_password
import notes.feature.login.generated.resources.login_hint_login_password_emoji
import notes.feature.login.generated.resources.login_hint_login_password_too_short
import notes.feature.login.generated.resources.login_hint_login_password_weak_type
import notes.feature.login.generated.resources.signup_username_hint
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

  /** 账号名不满足服务端的 `^[a-z0-9][a-z0-9._-]{2,63}$`。 */
  data object Invalid : AccountError
}

@Composable
fun AccountError?.text(): String = when (this) {
  is AccountError.Empty -> stringResource(Res.string.login_hint_login_account)
  is AccountError.Invalid -> stringResource(Res.string.signup_username_hint)
  null -> ""
}

/**
 * 密码错误
 */
sealed interface PasswordError : InputError {
  data object Empty : PasswordError
  data object TooShort : PasswordError
  data object WeakType : PasswordError

  /** 密码含表情符号，与服务端 `readNewPassword` 的拒绝范围一致。 */
  data object Emoji : PasswordError
}

@Composable
fun PasswordError?.text(): String = when (this) {
  is PasswordError.Empty -> stringResource(Res.string.login_hint_login_password)
  is PasswordError.TooShort -> stringResource(Res.string.login_hint_login_password_too_short)
  is PasswordError.WeakType -> stringResource(Res.string.login_hint_login_password_weak_type)
  is PasswordError.Emoji -> stringResource(Res.string.login_hint_login_password_emoji)
  null -> ""
}
