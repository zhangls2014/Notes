package me.zhangls.profile.mvi

internal object ProfileReducer {
  fun reduce(state: ProfileState, action: ProfileAction): ProfileState = when (action) {
    is ProfileAction.UpdateUser -> state.copy(
      user = action.user,
      edit = state.edit.takeIf { action.user?.id == state.user?.id && action.user?.authOrigin == state.user?.authOrigin },
    )
    is ProfileAction.UpdateEdit -> state.copy(edit = action.edit)
    is ProfileAction.UpdateSaveStatus -> state.copy(saveStatus = action.status)
  }
}
