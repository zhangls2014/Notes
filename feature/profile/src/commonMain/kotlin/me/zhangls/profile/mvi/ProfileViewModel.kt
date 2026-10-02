package me.zhangls.profile.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
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
  init {
    viewModelScope.launch {
      users.userFlow.collect { dispatch(ProfileAction.UpdateUser(it)) }
    }
  }

  override fun handleIntent(intent: ProfileIntent) {
    when (intent) {
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

  private fun dispatch(action: ProfileAction) {
    updateState { ProfileReducer.reduce(this, action) }
  }
}
