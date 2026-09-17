package me.zhangls.settings.mvi

object SettingsReducer {
  fun reduce(oldState: SettingsState, action: SettingsAction): SettingsState {
    return with(oldState) {
      when (action) {
        is SettingsAction.UpdateSettings -> copy(settings = action.settings)
        is SettingsAction.ShowDialog -> copy(dialog = action.dialog)
        SettingsAction.DismissDialog -> copy(dialog = null)
      }
    }
  }
}
