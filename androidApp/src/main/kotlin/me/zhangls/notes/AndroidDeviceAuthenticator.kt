package me.zhangls.notes

import android.app.Activity
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.zhangls.entry.DeviceAuthenticationResult
import me.zhangls.entry.DeviceAuthenticator
import me.zhangls.notes.AndroidAuthenticationMethod.Biometric
import me.zhangls.notes.AndroidAuthenticationMethod.Credential

internal class AndroidDeviceAuthenticator(
  private val activity: FragmentActivity,
  private val state: AndroidDeviceAuthenticationState,
) : DeviceAuthenticator {
  private val usesLegacyCredentialConfirmation = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
  private val keyguard = activity.getSystemService(KeyguardManager::class.java)
  private val credentialLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    val result = if (it.resultCode == Activity.RESULT_OK) {
      DeviceAuthenticationResult.Success
    } else {
      DeviceAuthenticationResult.Cancelled
    }
    state.finish(Credential, result)
  }
  private val prompt = BiometricPrompt(
    activity,
    ContextCompat.getMainExecutor(activity),
    object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        state.finish(Biometric, DeviceAuthenticationResult.Success)
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        if (usesLegacyCredentialConfirmation && errorCode in credentialFallbackErrors && state.switchToCredential()) {
          launchCredentialConfirmation()
          return
        }
        val result = when (errorCode) {
          BiometricPrompt.ERROR_CANCELED,
          BiometricPrompt.ERROR_USER_CANCELED,
          BiometricPrompt.ERROR_NEGATIVE_BUTTON -> DeviceAuthenticationResult.Cancelled
          BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> DeviceAuthenticationResult.NoDeviceCredential
          else -> DeviceAuthenticationResult.Unavailable
        }
        state.finish(Biometric, result)
      }
    },
  )

  override fun isDeviceCredentialMissing(): Boolean = keyguard?.isDeviceSecure == false

  override suspend fun authenticate(): DeviceAuthenticationResult = withContext(Dispatchers.Main.immediate) {
    if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      return@withContext DeviceAuthenticationResult.Unavailable
    }
    if (keyguard == null) return@withContext DeviceAuthenticationResult.Unavailable
    if (isDeviceCredentialMissing()) {
      return@withContext DeviceAuthenticationResult.NoDeviceCredential
    }
    val method = initialMethod()
    val attempt = state.start(method) ?: return@withContext DeviceAuthenticationResult.Unavailable
    try {
      when (method) {
        Biometric -> launchBiometricPrompt()
        Credential -> launchCredentialConfirmation()
      }
      attempt.await()
    } finally {
      if (attempt.isActive) {
        attempt.cancel()
        // Credential confirmation has no cancellation API. Keep its result slot until it returns.
        if (state.isBiometricAttempt(attempt)) prompt.cancelAuthentication()
      }
    }
  }

  private fun initialMethod(): AndroidAuthenticationMethod {
    if (!usesLegacyCredentialConfirmation) return Biometric
    return when (BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG)) {
      BiometricManager.BIOMETRIC_SUCCESS, BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> Biometric
      else -> Credential
    }
  }

  private fun launchBiometricPrompt() {
    val builder = BiometricPrompt.PromptInfo.Builder()
      .setTitle(activity.getString(R.string.device_authentication_title))
    if (usesLegacyCredentialConfirmation) {
      builder.setAllowedAuthenticators(BIOMETRIC_STRONG)
        .setNegativeButtonText(activity.getString(R.string.device_authentication_use_screen_lock))
    } else {
      builder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
    }
    prompt.authenticate(builder.build())
  }

  @Suppress("DEPRECATION") // Android 9–10 do not support BIOMETRIC_STRONG | DEVICE_CREDENTIAL.
  private fun launchCredentialConfirmation() {
    val intent = keyguard?.createConfirmDeviceCredentialIntent(
      activity.getString(R.string.device_authentication_title),
      null,
    )
    if (intent == null) {
      state.finish(Credential, DeviceAuthenticationResult.NoDeviceCredential)
      return
    }
    try {
      credentialLauncher.launch(intent)
    } catch (_: ActivityNotFoundException) {
      state.finish(Credential, DeviceAuthenticationResult.Unavailable)
    }
  }

  private companion object {
    val credentialFallbackErrors = setOf(
      BiometricPrompt.ERROR_NEGATIVE_BUTTON,
      BiometricPrompt.ERROR_LOCKOUT,
      BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
      BiometricPrompt.ERROR_HW_NOT_PRESENT,
      BiometricPrompt.ERROR_HW_UNAVAILABLE,
      BiometricPrompt.ERROR_NO_BIOMETRICS,
      BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED,
    )
  }
}
