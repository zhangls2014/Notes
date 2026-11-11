package me.zhangls.entry

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import me.zhangls.login.api.PasskeyProvider
import me.zhangls.login.api.PasskeyResult
import platform.AuthenticationServices.ASAuthorization
import platform.AuthenticationServices.ASAuthorizationController
import platform.AuthenticationServices.ASAuthorizationControllerDelegateProtocol
import platform.AuthenticationServices.ASAuthorizationControllerPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASAuthorizationErrorCanceled
import platform.AuthenticationServices.ASAuthorizationErrorDomain
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialAssertion
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialDescriptor
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialProvider
import platform.AuthenticationServices.ASAuthorizationPlatformPublicKeyCredentialRegistration
import platform.AuthenticationServices.ASAuthorizationPublicKeyCredentialAttestationKindNone
import platform.AuthenticationServices.ASAuthorizationPublicKeyCredentialUserVerificationPreferenceRequired
import platform.AuthenticationServices.ASAuthorizationRequest
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSJSONSerialization
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
internal class IosPasskeyProvider(private val host: UIViewController) : PasskeyProvider {
  private var activeRequest: AuthorizationDelegate? = null

  override suspend fun create(optionsJson: String): PasskeyResult {
    val options = optionsJson.jsonObject() ?: return PasskeyResult.Unavailable
    val rp = options["rp"] as? Map<*, *> ?: return PasskeyResult.Unavailable
    val rpId = rp["id"] as? String ?: return PasskeyResult.Unavailable
    val user = options["user"] as? Map<*, *> ?: return PasskeyResult.Unavailable
    val userId = (user["id"] as? String)?.decodeBase64Url() ?: return PasskeyResult.Unavailable
    val name = user["name"] as? String ?: return PasskeyResult.Unavailable
    val challenge = (options["challenge"] as? String)?.decodeBase64Url() ?: return PasskeyResult.Unavailable
    return authorize {
      ASAuthorizationPlatformPublicKeyCredentialProvider(rpId)
        .createCredentialRegistrationRequestWithChallenge(challenge, name, userId)
        .apply {
          displayName = user["displayName"] as? String
          attestationPreference = ASAuthorizationPublicKeyCredentialAttestationKindNone
          userVerificationPreference = ASAuthorizationPublicKeyCredentialUserVerificationPreferenceRequired
        }
    }
  }

  override suspend fun get(optionsJson: String): PasskeyResult {
    val options = optionsJson.jsonObject() ?: return PasskeyResult.Unavailable
    val rpId = options["rpId"] as? String ?: return PasskeyResult.Unavailable
    val challenge = (options["challenge"] as? String)?.decodeBase64Url() ?: return PasskeyResult.Unavailable
    val credentials = (options["allowCredentials"] as? List<*>).orEmpty().map { credential ->
      val id = ((credential as? Map<*, *>)?.get("id") as? String)?.decodeBase64Url()
        ?: return PasskeyResult.Unavailable
      ASAuthorizationPlatformPublicKeyCredentialDescriptor(id)
    }
    return authorize {
      ASAuthorizationPlatformPublicKeyCredentialProvider(rpId)
        .createCredentialAssertionRequestWithChallenge(challenge)
        .apply {
          allowedCredentials = credentials
          userVerificationPreference = ASAuthorizationPublicKeyCredentialUserVerificationPreferenceRequired
        }
    }
  }

  private suspend fun authorize(request: () -> ASAuthorizationRequest): PasskeyResult =
    withContext(Dispatchers.Main.immediate) {
      val window = host.viewIfLoaded?.window ?: return@withContext PasskeyResult.Unavailable
      if (activeRequest != null) return@withContext PasskeyResult.Unavailable
      val controller = ASAuthorizationController(listOf(request()))
      try {
        suspendCancellableCoroutine<PasskeyResult> { continuation ->
          val delegate = AuthorizationDelegate(window) { continuation.resume(it) }
          activeRequest = delegate
          controller.delegate = delegate
          controller.presentationContextProvider = delegate
          controller.performRequests()
        }
      } finally {
        val completed = activeRequest?.completed == true
        controller.delegate = null
        controller.presentationContextProvider = null
        activeRequest = null
        if (!completed) controller.cancel()
      }
    }
}

private class AuthorizationDelegate(
  private val window: UIWindow,
  private val onResult: (PasskeyResult) -> Unit,
) : NSObject(),
  ASAuthorizationControllerDelegateProtocol,
  ASAuthorizationControllerPresentationContextProvidingProtocol {
  var completed = false
    private set

  override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): UIWindow = window

  override fun authorizationController(
    controller: ASAuthorizationController,
    didCompleteWithAuthorization: ASAuthorization,
  ) {
    val result = when (val credential = didCompleteWithAuthorization.credential) {
      is ASAuthorizationPlatformPublicKeyCredentialRegistration -> {
        credential.rawAttestationObject?.let {
          credentialResult(
            credential.credentialID,
            mapOf("clientDataJSON" to credential.rawClientDataJSON.base64Url(), "attestationObject" to it.base64Url()),
          )
        } ?: PasskeyResult.Unavailable
      }
      is ASAuthorizationPlatformPublicKeyCredentialAssertion -> credential.authenticationResult()
      else -> PasskeyResult.Unavailable
    }
    finish(result)
  }

  override fun authorizationController(controller: ASAuthorizationController, didCompleteWithError: NSError) {
    val cancelled = didCompleteWithError.domain == ASAuthorizationErrorDomain &&
      didCompleteWithError.code == ASAuthorizationErrorCanceled
    finish(if (cancelled) PasskeyResult.Cancelled else PasskeyResult.Unavailable)
  }

  private fun finish(result: PasskeyResult) {
    if (completed) return
    completed = true
    onResult(result)
  }
}

private fun ASAuthorizationPlatformPublicKeyCredentialAssertion.authenticationResult(): PasskeyResult {
  val authenticatorData = rawAuthenticatorData ?: return PasskeyResult.Unavailable
  val signatureData = signature ?: return PasskeyResult.Unavailable
  val userHandle = userID ?: return PasskeyResult.Unavailable
  return credentialResult(
    credentialID,
    mapOf(
      "clientDataJSON" to rawClientDataJSON.base64Url(),
      "authenticatorData" to authenticatorData.base64Url(),
      "signature" to signatureData.base64Url(),
      "userHandle" to userHandle.base64Url(),
    ),
  )
}

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private fun String.jsonObject(): Map<*, *>? {
  val utf8 = encodeToByteArray()
  if (utf8.isEmpty()) return null
  val data = utf8.usePinned { NSData.create(bytes = it.addressOf(0), length = utf8.size.toULong()) }
  return NSJSONSerialization.JSONObjectWithData(data, options = 0UL, error = null) as? Map<*, *>
}

@OptIn(BetaInteropApi::class)
private fun String.decodeBase64Url(): NSData? {
  if (isEmpty() || length % 4 == 1) return null
  val base64 = replace('-', '+').replace('_', '/').padEnd((length + 3) / 4 * 4, '=')
  return NSData.create(base64EncodedString = base64, options = 0UL)
}

private fun NSData.base64Url(): String =
  base64EncodedStringWithOptions(0UL).replace('+', '-').replace('/', '_').trimEnd('=')

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
private fun credentialResult(id: NSData, response: Map<String, String>): PasskeyResult {
  val encodedId = id.base64Url()
  val credential = mapOf(
    "id" to encodedId,
    "rawId" to encodedId,
    "type" to "public-key",
    "response" to response,
    "clientExtensionResults" to emptyMap<String, String>(),
  )
  val data = NSJSONSerialization.dataWithJSONObject(credential, options = 0UL, error = null)
    ?: return PasskeyResult.Unavailable
  val json = data.bytes?.readBytes(data.length.toInt())?.decodeToString()
  return json?.let(PasskeyResult::Success) ?: PasskeyResult.Unavailable
}
