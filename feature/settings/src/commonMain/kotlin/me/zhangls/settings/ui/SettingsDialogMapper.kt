package me.zhangls.settings.ui

import me.zhangls.settings.domain.SettingsDialog
import notes.feature.settings.generated.resources.Res
import notes.feature.settings.generated.resources.settings_dialog_action_cancel
import notes.feature.settings.generated.resources.settings_dialog_action_logout
import notes.feature.settings.generated.resources.settings_dialog_content_logout
import notes.feature.settings.generated.resources.settings_dialog_label_logout

/**
 * 将领域对话框类型 [SettingsDialog] 映射为携带文案资源的表现层 [DialogUiModel]。
 *
 * @author zhangls
 */
internal fun SettingsDialog.toDialogUiModel(): DialogUiModel {
  return when (this) {
    SettingsDialog.Logout -> DialogUiModel(
      dialogId = dialogId,
      title = Res.string.settings_dialog_label_logout,
      message = Res.string.settings_dialog_content_logout,
      confirm = Res.string.settings_dialog_action_logout,
      dismiss = Res.string.settings_dialog_action_cancel,
    )
  }
}
