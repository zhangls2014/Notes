package me.zhangls.settings.ui

import org.jetbrains.compose.resources.StringResource

/**
 * 对话框的表现层模型：持有标题、正文、按钮等文案资源，
 * 由 UI 层负责解析显示。
 *
 * @author zhangls
 */
internal data class DialogUiModel(
  val dialogId: String,
  val title: StringResource,
  val message: StringResource,
  val confirm: StringResource,
  val dismiss: StringResource? = null,
)
