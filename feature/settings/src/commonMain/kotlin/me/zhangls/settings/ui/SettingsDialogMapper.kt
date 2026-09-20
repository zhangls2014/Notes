package me.zhangls.settings.ui

import me.zhangls.preference.LogoutPreference
import me.zhangls.settings.domain.SettingsDialog
import notes.feature.settings.generated.resources.Res
import notes.feature.settings.generated.resources.settings_dialog_action_cancel
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
      // 确认按钮文案直接复用设置项的标题（同为"退出登录"）：两者语义同源，
      // 各写一份的结果是改标题时按钮不会跟着变。跨模块取 core:preference 的
      // 公开 API 是可行的，取它的字符串资源则不行（生成器产出的是 internal）。
      confirm = LogoutPreference.spec.title,
      dismiss = Res.string.settings_dialog_action_cancel,
    )
  }
}
