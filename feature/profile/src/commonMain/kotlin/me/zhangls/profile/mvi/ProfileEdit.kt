package me.zhangls.profile.mvi

import androidx.compose.foundation.text.input.TextFieldState

internal enum class ProfileField { Username, Email }

internal enum class ProfileEditError { Save, VerifyPassword, IncorrectPassword, SendCode }

internal enum class ProfileEditStep { SaveUsername, VerifyPassword, SendCode, SaveEmail }

internal const val EMAIL_CODE_LENGTH = 6

internal data class ProfileEdit(
  val field: ProfileField,
  val accountId: String,
  val originalValue: String = "",
  val username: String = originalValue,
  val password: String = "",
  val passwordVerified: Boolean = false,
  val email: String = "",
  val sentEmail: String? = null,
  val resendCoolingDown: Boolean = false,
  val code: String = "",
  val busy: Boolean = false,
  val error: ProfileEditError? = null,
  val usernameInput: TextFieldState = TextFieldState(username),
  val passwordInput: TextFieldState = TextFieldState(password),
  val emailInput: TextFieldState = TextFieldState(email),
  val codeInput: TextFieldState = TextFieldState(code),
) {
  val recipient: String get() = email.trim().let { address ->
    val domainStart = address.lastIndexOf('@') + 1
    if (domainStart == 0) address else address.take(domainStart) + address.drop(domainStart).lowercase()
  }
  val codeRequested: Boolean get() = sentEmail != null && sentEmail == recipient

  val nextStep: ProfileEditStep? get() = when {
    busy -> null
    this.field == ProfileField.Username -> ProfileEditStep.SaveUsername.takeIf {
      username.isNotBlank() && username.trim().lowercase() != originalValue
    }
    !passwordVerified -> ProfileEditStep.VerifyPassword.takeIf { password.isNotEmpty() }
    recipient.isBlank() || recipient == originalValue -> null
    !codeRequested -> ProfileEditStep.SendCode
    code.length == EMAIL_CODE_LENGTH -> ProfileEditStep.SaveEmail
    code.isEmpty() && !resendCoolingDown -> ProfileEditStep.SendCode
    else -> null
  }

  override fun toString() = "ProfileEdit(field=$field, busy=$busy)"
}
