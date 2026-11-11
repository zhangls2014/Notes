package me.zhangls.login.api

import androidx.compose.runtime.staticCompositionLocalOf

interface PasswordlessAuth {
  /** Calls [onPasswordVerified] after server verification and before publishing the session. */
  suspend fun signInWithPassword(account: String, password: String, onPasswordVerified: (() -> Unit)? = null)
  suspend fun signInWithPasskey(provider: PasskeyProvider): Boolean
}

val LocalPasswordlessAuth = staticCompositionLocalOf<PasswordlessAuth?> { null }
val LocalPasskeyProvider = staticCompositionLocalOf<PasskeyProvider?> { null }

class PasskeyUnavailableException : Exception()
