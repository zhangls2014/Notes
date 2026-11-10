package me.zhangls.entry

fun interface DeviceAuthenticator {
  suspend fun authenticate(): DeviceAuthenticationResult

  /** Returns true only when the system confirms there is no secure screen lock. */
  fun isDeviceCredentialMissing(): Boolean = false
}

enum class DeviceAuthenticationResult {
  Success,
  Cancelled,
  Unavailable,
  NoDeviceCredential,
}

internal val UnavailableDeviceAuthenticator = DeviceAuthenticator { DeviceAuthenticationResult.Unavailable }
