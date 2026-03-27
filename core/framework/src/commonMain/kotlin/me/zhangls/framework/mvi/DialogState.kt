package me.zhangls.framework.mvi

import kotlinx.serialization.Serializable

/**
 * @author zhangls
 */
@Serializable
data class DialogState(
  val dialogId: String,
  val title: String,
  val message: String,
  val confirm: String,
  val dismiss: String? = null,
)

sealed interface DialogResult {
  val dialogId: String

  data class Confirm(override val dialogId: String) : DialogResult
  data class Dismiss(override val dialogId: String) : DialogResult
}
