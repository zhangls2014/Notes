package me.zhangls.settings.domain

import kotlinx.serialization.Serializable

/**
 * 设置页对话框的领域语义类型：只描述"是哪种对话框"，
 * 不携带任何 UI 资源；标题、文案等资源由 UI 层映射提供。
 *
 * @author zhangls
 */
@Serializable
sealed interface SettingsDialog {
  val dialogId: String

  @Serializable
  data object Logout : SettingsDialog {
    override val dialogId = "dialog_id_logout"
  }
}
