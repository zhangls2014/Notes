package me.zhangls.framework.mvi

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.annotation.Singleton

@Singleton
class ToastGlobalNotifier {
  companion object {
    // 缓存 Toast 的数量
    private const val TOAST_BUFFER_SIZE = 16

    // 相同 Toast 的显示间隔
    private const val TOAST_INTERVAL = 2_000_000_000L
  }

  private val _toast = MutableSharedFlow<ToastEffect>(
    extraBufferCapacity = TOAST_BUFFER_SIZE,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
  )
  val toast = _toast.asSharedFlow()

  /**
   * 记录上次显示的 Toast 信息
   */
  @Volatile
  private var lastToastEffect: ToastEffect? = null

  fun showToast(resId: Int) {
    val effect = ToastEffect(resId)

    if (checkToast(effect)) {
      lastToastEffect = effect
      _toast.tryEmit(effect)
    }
  }

  /**
   * 检查 Toast 是否允许显示
   */
  private fun checkToast(effect: ToastEffect): Boolean {
    lastToastEffect?.let {
      if (it.resId == effect.resId) {
        if (effect.timestamp - it.timestamp < TOAST_INTERVAL) {
          return false
        }
      }
    }
    return true
  }
}