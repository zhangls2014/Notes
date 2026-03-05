package me.zhangls.framework.mvi

import kotlinx.serialization.Serializable

/**
 * @author zhangls
 */
@Serializable
data class DialogState(
  val dialogId: String,
  val title: Int,
  val message: Int,
  val confirm: Int,
  val dismiss: Int? = null,
)

sealed interface DialogResult {
  val dialogId: String

  data class Confirm(override val dialogId: String) : DialogResult
  data class Dismiss(override val dialogId: String) : DialogResult
}
