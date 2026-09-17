package me.zhangls.settings.mvi

import me.zhangls.data.model.SettingsModel
import me.zhangls.framework.mvi.MviAction
import me.zhangls.settings.domain.SettingsDialog

sealed interface SettingsAction : MviAction {
  data class UpdateSettings(val settings: SettingsModel) : SettingsAction

  data class ShowDialog(val dialog: SettingsDialog) : SettingsAction

  data object DismissDialog : SettingsAction
}
