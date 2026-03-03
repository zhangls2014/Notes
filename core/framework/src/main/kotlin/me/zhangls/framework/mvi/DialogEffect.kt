package me.zhangls.framework.mvi


/**
 * @author zhangls
 */
data class DialogEffect(
  val dialogId: String,
  val title: Int,
  val message: Int,
  val confirm: Int,
  val dismiss: Int? = null,
) : MviEffect

sealed interface DialogResult {
  val dialogId: String

  data class Confirm(override val dialogId: String) : DialogResult
  data class Dismiss(override val dialogId: String) : DialogResult
}