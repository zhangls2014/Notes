package me.zhangls.profile.mvi

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.profile.AvatarUpdater
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class ProfileViewModel(
  savedStateHandle: SavedStateHandle,
  private val users: UserRepository,
  private val avatars: AvatarUpdater,
) : MviViewModel<ProfileState, ProfileIntent>(
  initialState = ProfileState(),
  stateSerializer = ProfileState.serializer(),
  savedStateHandle = savedStateHandle,
) {
  private var inputJob: Job? = null
  private var editJob: Job? = null
  private var resendCooldown: Job? = null

  init {
    viewModelScope.launch { avatars.refresh() }
    viewModelScope.launch {
      users.userFlow.collect { user ->
        val previous = state.value.user
        if (user?.id != previous?.id || user?.authOrigin != previous?.authOrigin) {
          editJob?.cancel()
          closeEdit()
        }
        dispatch(ProfileAction.UpdateUser(user))
      }
    }
  }

  override fun handleIntent(intent: ProfileIntent) {
    when (intent) {
      is ProfileIntent.OpenEdit -> openEdit(intent.field)
      ProfileIntent.CloseEdit -> {
        if (state.value.edit?.busy != true) closeEdit()
      }
      is ProfileIntent.UpdateUsername -> editDraft { copy(username = intent.value, error = null) }
      is ProfileIntent.UpdatePassword -> editDraft {
        if (passwordVerified) this else copy(password = intent.value, error = null)
      }
      is ProfileIntent.UpdateEmail -> editDraft {
        val updated = copy(email = intent.value, error = null)
        if (updated.recipient == recipient) updated else updated.copy(sentEmail = null, code = "")
      }
      is ProfileIntent.UpdateCode -> editDraft {
        copy(code = intent.value.filter { it in '0'..'9' }.take(EMAIL_CODE_LENGTH), error = null)
      }
      ProfileIntent.ConfirmEdit -> confirmEdit()
      is ProfileIntent.ChangeAvatar -> {
        val file = intent.file ?: return
        if (state.value.user == null || state.value.saveStatus == AvatarSaveStatus.Saving) return
        dispatch(ProfileAction.UpdateSaveStatus(AvatarSaveStatus.Saving))
        viewModelScope.launch {
          try {
            avatars.update(file)
            dispatch(ProfileAction.UpdateSaveStatus(AvatarSaveStatus.Saved))
          } catch (cancelled: CancellationException) {
            throw cancelled
          } catch (_: Exception) {
            dispatch(ProfileAction.UpdateSaveStatus(AvatarSaveStatus.Failed))
          }
        }
      }
    }
  }

  private fun openEdit(field: ProfileField) {
    val current = state.value
    val user = current.user?.takeIf { it.authOrigin != null } ?: return
    if (current.edit?.busy == true || current.edit?.field == field) return
    closeEdit()
    val original = if (field == ProfileField.Username) user.nickname else user.email.orEmpty()
    val edit = ProfileEdit(field, user.id, original, username = user.nickname)
    dispatch(ProfileAction.UpdateEdit(edit))
    inputJob = viewModelScope.launch {
      snapshotFlow {
        edit.copy(
          username = edit.usernameInput.text.toString(),
          password = edit.passwordInput.text.toString(),
          email = edit.emailInput.text.toString(),
          code = edit.codeInput.text.toString(),
        )
      }.collect { values ->
        val current = state.value.edit
        if (current != null && !current.busy) {
          if (values.username != current.username) handleIntent(ProfileIntent.UpdateUsername(values.username))
          if (values.password != current.password) handleIntent(ProfileIntent.UpdatePassword(values.password))
          if (values.email != current.email) handleIntent(ProfileIntent.UpdateEmail(values.email))
          if (values.code != current.code) handleIntent(ProfileIntent.UpdateCode(values.code))
        }
      }
    }
  }

  private fun closeEdit() {
    inputJob?.cancel()
    state.value.edit?.let {
      it.passwordInput.setTextAndPlaceCursorAtEnd("")
      it.codeInput.setTextAndPlaceCursorAtEnd("")
    }
    resendCooldown?.cancel()
    dispatch(ProfileAction.UpdateEdit(null))
  }

  private fun editDraft(transform: ProfileEdit.() -> ProfileEdit) {
    val edit = state.value.edit?.takeUnless { it.busy } ?: return
    dispatch(ProfileAction.UpdateEdit(edit.transform().also { syncEditor(edit, it) }))
  }

  private fun syncEditor(previous: ProfileEdit, edit: ProfileEdit) {
    listOf(
      Triple(edit.usernameInput, previous.username, edit.username),
      Triple(edit.passwordInput, previous.password, edit.password),
      Triple(edit.emailInput, previous.email, edit.email),
      Triple(edit.codeInput, previous.code, edit.code),
    ).forEach { (input, old, value) ->
      if (old != value && input.text.toString() != value) input.setTextAndPlaceCursorAtEnd(value)
    }
  }

  private fun confirmEdit() {
    val current = state.value.edit ?: return
    val edit = current.copy(
      username = current.usernameInput.text.toString(),
      password = current.passwordInput.text.toString(),
      email = current.emailInput.text.toString(),
      code = current.codeInput.text.toString(),
    )
    val step = edit.nextStep ?: return
    dispatch(ProfileAction.UpdateEdit(edit.copy(busy = true, error = null)))
    editJob = viewModelScope.launch {
      try {
        when (step) {
          ProfileEditStep.SaveUsername -> {
            avatars.updateUsername(edit.accountId, edit.username)
            closeEdit()
          }
          ProfileEditStep.VerifyPassword -> verifyPassword(edit)
          ProfileEditStep.SendCode -> sendCode(edit)
          ProfileEditStep.SaveEmail -> {
            avatars.changeEmail(edit.accountId, edit.recipient, edit.code)
            closeEdit()
          }
        }
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        val error = when (step) {
          ProfileEditStep.VerifyPassword -> ProfileEditError.VerifyPassword
          ProfileEditStep.SendCode -> ProfileEditError.SendCode
          else -> ProfileEditError.Save
        }
        updateCurrentEdit { copy(busy = false, error = error) }
      }
    }
  }

  private suspend fun verifyPassword(edit: ProfileEdit) {
    val verified = avatars.verifyEmailChangePassword(edit.accountId, edit.password)
    updateCurrentEdit {
      copy(
        busy = false,
        passwordVerified = verified,
        error = ProfileEditError.IncorrectPassword.takeUnless { verified },
      )
    }
  }

  private suspend fun sendCode(edit: ProfileEdit) {
    avatars.sendEmailChangeCode(edit.accountId, edit.recipient, edit.password)
    resendCooldown?.cancel()
    updateCurrentEdit { copy(busy = false, sentEmail = edit.recipient, code = "", resendCoolingDown = true) }
    resendCooldown = viewModelScope.launch {
      delay(60_000)
      updateCurrentEdit { copy(resendCoolingDown = false) }
    }
  }

  private fun updateCurrentEdit(transform: ProfileEdit.() -> ProfileEdit) {
    val edit = state.value.edit ?: return
    dispatch(ProfileAction.UpdateEdit(edit.transform().also { syncEditor(edit, it) }))
  }

  private fun dispatch(action: ProfileAction) {
    updateState { ProfileReducer.reduce(this, action) }
  }
}
