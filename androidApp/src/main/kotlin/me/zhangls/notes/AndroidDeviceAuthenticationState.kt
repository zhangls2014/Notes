package me.zhangls.notes

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CompletableDeferred
import me.zhangls.entry.DeviceAuthenticationResult

internal enum class AndroidAuthenticationMethod {
  Biometric,
  Credential,
}

/** Keeps the outstanding system request associated with its caller across Activity recreation. */
internal class AndroidDeviceAuthenticationState : ViewModel() {
  private var method: AndroidAuthenticationMethod? = null
  private var attempt: CompletableDeferred<DeviceAuthenticationResult>? = null

  fun start(method: AndroidAuthenticationMethod): CompletableDeferred<DeviceAuthenticationResult>? {
    if (attempt != null) return null
    this.method = method
    return CompletableDeferred<DeviceAuthenticationResult>().also { attempt = it }
  }

  fun switchToCredential(): Boolean {
    if (method != AndroidAuthenticationMethod.Biometric || attempt?.isActive != true) return false
    method = AndroidAuthenticationMethod.Credential
    return true
  }

  fun isBiometricAttempt(request: CompletableDeferred<DeviceAuthenticationResult>): Boolean =
    attempt === request && method == AndroidAuthenticationMethod.Biometric

  fun finish(method: AndroidAuthenticationMethod, result: DeviceAuthenticationResult) {
    if (this.method != method) return
    val pending = attempt
    attempt = null
    this.method = null
    pending?.complete(result)
  }

  override fun onCleared() {
    attempt?.complete(DeviceAuthenticationResult.Cancelled)
  }
}
