package me.zhangls.entry

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorAppCancel
import platform.LocalAuthentication.LAErrorPasscodeNotSet
import platform.LocalAuthentication.LAErrorSystemCancel
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import kotlin.coroutines.resume

internal object IosDeviceAuthenticator : DeviceAuthenticator {
  override fun isDeviceCredentialMissing(): Boolean {
    val context = LAContext()
    return try {
      context.unavailability() == DeviceAuthenticationResult.NoDeviceCredential
    } finally {
      context.invalidate()
    }
  }

  override suspend fun authenticate(): DeviceAuthenticationResult = withContext(Dispatchers.Main.immediate) {
    val context = LAContext()
    try {
      context.unavailability() ?: suspendCancellableCoroutine { continuation ->
        val reason = NSBundle.mainBundle.localizedStringForKey("DeviceAuthenticationReason", null, null)
        context.evaluatePolicy(LAPolicyDeviceOwnerAuthentication, localizedReason = reason) { success, error ->
          continuation.resume(if (success) DeviceAuthenticationResult.Success else error.toAuthenticationResult())
        }
      }
    } finally {
      context.invalidate()
    }
  }

  @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
  private fun LAContext.unavailability(): DeviceAuthenticationResult? = memScoped {
    val error = alloc<ObjCObjectVar<NSError?>>()
    error.value = null
    if (canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error.ptr)) {
      null
    } else {
      error.value.toAuthenticationResult()
    }
  }

  private fun NSError?.toAuthenticationResult(): DeviceAuthenticationResult = when (this?.code) {
    LAErrorPasscodeNotSet -> DeviceAuthenticationResult.NoDeviceCredential
    LAErrorAppCancel, LAErrorSystemCancel, LAErrorUserCancel -> DeviceAuthenticationResult.Cancelled
    else -> DeviceAuthenticationResult.Unavailable
  }
}
