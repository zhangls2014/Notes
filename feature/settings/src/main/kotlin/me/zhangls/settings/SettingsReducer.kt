package me.zhangls.settings

object SettingsReducer {
  fun reduce(oldState: SettingsState, action: SettingsAction): SettingsState {
    return with(oldState) {
      when (action) {
        is SettingsAction.UpdatePreferences -> copy(preferences = action.preferences)
        is SettingsAction.ShowDialog -> copy(dialog = action.dialog)
        SettingsAction.DismissDialog -> copy(dialog = null)
      }
    }
  }
}
