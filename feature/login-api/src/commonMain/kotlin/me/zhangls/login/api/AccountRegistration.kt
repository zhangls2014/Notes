package me.zhangls.login.api

import androidx.compose.runtime.staticCompositionLocalOf

interface AccountRegistration {
  suspend fun sendSignupCode(email: String)
  suspend fun verifySignupCode(email: String, code: String): String
  suspend fun registerAccount(email: String, proof: String, username: String, password: String)
}

class EmailAlreadyRegisteredException : Exception()

val LocalAccountRegistration = staticCompositionLocalOf<AccountRegistration?> { null }
