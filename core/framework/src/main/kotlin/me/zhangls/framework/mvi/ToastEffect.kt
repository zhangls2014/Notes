package me.zhangls.framework.mvi

import android.widget.Toast

/**
 * @author zhangls
 */
data class ToastEffect(val message: String) : MviEffect {
  /**
   * 显示时长
   */
  fun duration(): Int = if (message.length <= 20) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
}
