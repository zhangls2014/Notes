package me.zhangls.login.api

interface PasskeyProvider {
  val isSupported: Boolean get() = true

  suspend fun create(optionsJson: String): PasskeyResult
  suspend fun get(optionsJson: String): PasskeyResult
}

sealed interface PasskeyResult {
  class Success(val credentialJson: String) : PasskeyResult
  data object Cancelled : PasskeyResult
  data object Unavailable : PasskeyResult
}

class PasskeyCreationUnavailableException : Exception()

class PasskeyCreationTimedOutException : Exception()
