package me.zhangls.profile.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.zhangls.login.api.PasskeyAlreadyExistsException
import me.zhangls.login.api.PasskeyCreationTimedOutException
import me.zhangls.login.api.PasskeyCreationUnavailableException
import me.zhangls.profile.api.PasskeyManagement
import me.zhangls.profile.api.PasskeySessionVerificationRequiredException
import me.zhangls.profile.api.RegisteredPasskey
import notes.feature.profile.generated.resources.Res
import notes.feature.profile.generated.resources.auth_passkey_add_error
import notes.feature.profile.generated.resources.auth_passkey_already_exists
import notes.feature.profile.generated.resources.auth_passkey_error
import notes.feature.profile.generated.resources.auth_passkey_no_service
import notes.feature.profile.generated.resources.auth_passkey_recent
import notes.feature.profile.generated.resources.auth_passkey_remove_error
import notes.feature.profile.generated.resources.auth_passkey_timeout
import org.jetbrains.compose.resources.StringResource

internal data class PasskeyManagementState(
  val passkeys: List<RegisteredPasskey>? = null,
  val error: StringResource? = null,
  val busy: Boolean = false,
  val pendingRevocation: RegisteredPasskey? = null,
)

internal class PasskeyManagementViewModel(
  private val management: PasskeyManagement,
) : ViewModel() {
  private val mutableState = MutableStateFlow(PasskeyManagementState())
  val state = mutableState.asStateFlow()

  init {
    reload()
  }

  fun reload() = submit(Res.string.auth_passkey_error)

  fun add(operation: suspend () -> Boolean) = submit(Res.string.auth_passkey_add_error, operation)

  fun requestRevocation(passkey: RegisteredPasskey) {
    mutableState.update { if (it.busy) it else it.copy(pendingRevocation = passkey) }
  }

  fun dismissRevocation() {
    mutableState.update { it.copy(pendingRevocation = null) }
  }

  fun confirmRevocation() {
    val passkey = state.value.pendingRevocation ?: return
    dismissRevocation()
    submit(Res.string.auth_passkey_remove_error) {
      management.revoke(passkey.id)
      mutableState.update { it.copy(passkeys = it.passkeys?.filterNot { key -> key.id == passkey.id }) }
      true
    }
  }

  private fun submit(failureMessage: StringResource, operation: suspend () -> Boolean = { true }) {
    if (state.value.busy) return
    mutableState.update { it.copy(busy = true, error = null) }
    viewModelScope.launch {
      var failedAction = failureMessage
      try {
        if (operation()) {
          failedAction = Res.string.auth_passkey_error
          val passkeys = management.list()
          mutableState.update { it.copy(passkeys = passkeys) }
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (failure: Exception) {
        mutableState.update { it.copy(error = failure.messageResource(failedAction)) }
      } finally {
        mutableState.update { it.copy(busy = false) }
      }
    }
  }
}

private fun Exception.messageResource(fallback: StringResource): StringResource = when (this) {
  is PasskeyAlreadyExistsException -> Res.string.auth_passkey_already_exists
  is PasskeyCreationUnavailableException -> Res.string.auth_passkey_no_service
  is PasskeyCreationTimedOutException -> Res.string.auth_passkey_timeout
  is PasskeySessionVerificationRequiredException -> Res.string.auth_passkey_recent
  else -> fallback
}
