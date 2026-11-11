package me.zhangls.notes

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import me.zhangls.login.api.PasskeyProvider
import me.zhangls.login.api.PasskeyResult

internal class AndroidPasskeyProvider(private val activity: Activity) : PasskeyProvider {
  override val isSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
      activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CREDENTIALS)

  private val credentialManager = CredentialManager.create(activity.applicationContext)

  override suspend fun create(optionsJson: String): PasskeyResult = try {
    val response = credentialManager.createCredential(activity, CreatePublicKeyCredentialRequest(optionsJson))
    (response as? CreatePublicKeyCredentialResponse)
      ?.let { PasskeyResult.Success(it.registrationResponseJson) }
      ?: PasskeyResult.Unavailable
  } catch (_: CreateCredentialCancellationException) {
    PasskeyResult.Cancelled
  } catch (_: CreateCredentialException) {
    PasskeyResult.Unavailable
  } catch (_: IllegalArgumentException) {
    PasskeyResult.Unavailable
  }

  override suspend fun get(optionsJson: String): PasskeyResult = try {
    val request = GetCredentialRequest(listOf(GetPublicKeyCredentialOption(optionsJson)))
    val credential = credentialManager.getCredential(activity, request).credential
    (credential as? PublicKeyCredential)
      ?.let { PasskeyResult.Success(it.authenticationResponseJson) }
      ?: PasskeyResult.Unavailable
  } catch (_: GetCredentialCancellationException) {
    PasskeyResult.Cancelled
  } catch (_: GetCredentialException) {
    PasskeyResult.Unavailable
  } catch (_: IllegalArgumentException) {
    PasskeyResult.Unavailable
  }
}
