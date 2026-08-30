package me.zhangls.settings

import me.zhangls.data.model.SettingsModel
import me.zhangls.framework.mvi.DialogState
import me.zhangls.framework.mvi.MviAction

sealed interface SettingsAction : MviAction {
  data class UpdateSettings(val settings: SettingsModel) : SettingsAction

  data class ShowDialog(val dialog: DialogState) : SettingsAction

  data object DismissDialog : SettingsAction
}
