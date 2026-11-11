package me.zhangls.login.api

import androidx.compose.runtime.staticCompositionLocalOf

interface PasswordlessAuth {
  suspend fun signInWithPassword(account: String, password: String)
  suspend fun signInWithPasskey(provider: PasskeyProvider): Boolean
}

val LocalPasswordlessAuth = staticCompositionLocalOf<PasswordlessAuth?> { null }
val LocalPasskeyProvider = staticCompositionLocalOf<PasskeyProvider?> { null }

class PasskeyUnavailableException : Exception()
