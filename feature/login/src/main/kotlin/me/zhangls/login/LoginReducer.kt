package me.zhangls.login

import me.zhangls.login.domain.LoginValidator

object LoginReducer {
  fun reduce(oldState: LoginState, action: LoginAction): LoginState {
    return with(oldState) {
      when (action) {
        is LoginAction.ValidationResult -> {
          copy(
            accountError = LoginValidator.validateAccount(account),
            passwordError = LoginValidator.validatePassword(password),
            isInputValid = LoginValidator.validateAll(account, password)
          )
        }

        is LoginAction.UpdateAccount -> {
          val newAccount = action.account
          copy(
            account = newAccount,
            accountError = LoginValidator.validateAccount(newAccount),
            isInputValid = LoginValidator.validateAll(newAccount, password)
          )
        }

        is LoginAction.ClearAccount -> {
          copy(account = "", accountError = null, isInputValid = false)
        }

        is LoginAction.UpdatePassword -> {
          val newPassword = action.password
          copy(
            password = newPassword,
            passwordError = LoginValidator.validatePassword(newPassword),
            isInputValid = LoginValidator.validateAll(account, newPassword)
          )
        }

        is LoginAction.UpdatePasswordVisible -> {
          copy(passwordVisible = action.visible)
        }

        is LoginAction.Loading -> {
          copy(isLoading = action.visible)
        }
      }
    }
  }
}