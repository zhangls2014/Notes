package me.zhangls.profile.mvi

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
  private var editJob: Job? = null
  private var resendCooldown: Job? = null

  init {
    viewModelScope.launch { avatars.refresh() }
    viewModelScope.launch {
      users.userFlow.collect { user ->
        val previous = state.value.user
        if (user?.id != previous?.id || user?.authOrigin != previous?.authOrigin) {
          editJob?.cancel()
          resendCooldown?.cancel()
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
    dispatch(ProfileAction.UpdateEdit(ProfileEdit(field, user.id, original, username = user.nickname)))
  }

  private fun closeEdit() {
    resendCooldown?.cancel()
    dispatch(ProfileAction.UpdateEdit(null))
  }

  private fun editDraft(transform: ProfileEdit.() -> ProfileEdit) {
    val edit = state.value.edit?.takeUnless { it.busy } ?: return
    dispatch(ProfileAction.UpdateEdit(edit.transform()))
  }

  private fun confirmEdit() {
    val edit = state.value.edit ?: return
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
    dispatch(ProfileAction.UpdateEdit(edit.transform()))
  }

  private fun dispatch(action: ProfileAction) {
    updateState { ProfileReducer.reduce(this, action) }
  }
}
