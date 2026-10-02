package me.zhangls.profile.mvi

internal object ProfileReducer {
  fun reduce(state: ProfileState, action: ProfileAction): ProfileState = when (action) {
    is ProfileAction.UpdateUser -> state.copy(user = action.user)
    is ProfileAction.UpdateSaveStatus -> state.copy(saveStatus = action.status)
  }
}
