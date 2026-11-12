package me.zhangls.login.mvi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.zhangls.login.api.AccountRegistration
import me.zhangls.login.domain.LoginValidator
import notes.feature.login.generated.resources.Res
import notes.feature.login.generated.resources.signup_create_failed
import notes.feature.login.generated.resources.signup_send_failed
import notes.feature.login.generated.resources.signup_verify_failed
import org.jetbrains.compose.resources.StringResource

private const val RESEND_SECONDS = 60
private const val SECOND_MS = 1000L

/** The navigation entry retains this draft in memory across configuration changes. */
internal class RegistrationViewModel : ViewModel() {
  var email by mutableStateOf("")
  var code by mutableStateOf("")
  var username by mutableStateOf("")
  var password by mutableStateOf("")
  var passwordVisible by mutableStateOf(false)

  private var sentEmail by mutableStateOf<String?>(null)
  val codeSent: Boolean get() = sentEmail == email.trim()

  private var proof by mutableStateOf<String?>(null)
  val emailVerified: Boolean get() = proof != null

  var resendIn by mutableIntStateOf(0)
    private set
  var busy by mutableStateOf(false)
    private set
  var error by mutableStateOf<StringResource?>(null)
    private set
  var registered by mutableStateOf(false)
    private set

  fun sendCode(auth: AccountRegistration) {
    if (resendIn > 0 || email.isBlank()) return
    request(Res.string.signup_send_failed) {
      val recipient = email.trim()
      auth.sendSignupCode(recipient)
      sentEmail = recipient
      code = ""
      resendIn = RESEND_SECONDS
      viewModelScope.launch {
        while (resendIn > 0) {
          delay(SECOND_MS)
          resendIn--
        }
      }
    }
  }

  fun verifyCode(auth: AccountRegistration) = request(Res.string.signup_verify_failed) {
    proof = auth.verifySignupCode(email.trim(), code)
  }

  fun register(auth: AccountRegistration) {
    val verified = proof ?: return
    request(Res.string.signup_create_failed) {
      auth.registerAccount(email.trim(), verified, LoginValidator.normalizeAccount(username), password)
      registered = true
    }
  }

  fun backToEmail() {
    if (busy) return
    proof = null
    code = ""
    username = ""
    password = ""
    passwordVisible = false
    error = null
  }

  private fun request(failureMessage: StringResource, action: suspend () -> Unit) {
    if (busy || registered) return
    busy = true
    error = null
    viewModelScope.launch {
      try {
        action()
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        error = failureMessage
      } finally {
        busy = false
      }
    }
  }
}
