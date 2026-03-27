package me.zhangls.settings

import me.zhangls.framework.mvi.DialogState
import me.zhangls.framework.mvi.MviAction
import me.zhangls.settings.domain.Preference

sealed interface SettingsAction : MviAction {
  data class UpdatePreferences(val preferences: List<Preference<*>>) : SettingsAction

  data class ShowDialog(val dialog: DialogState) : SettingsAction

  data object DismissDialog : SettingsAction
}
