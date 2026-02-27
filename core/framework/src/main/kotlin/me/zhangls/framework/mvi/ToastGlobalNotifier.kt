package me.zhangls.framework.mvi

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToastGlobalNotifier @Inject constructor() {
  private val _toast = MutableSharedFlow<ToastEffect>(extraBufferCapacity = 16)
  val toast = _toast.asSharedFlow()

  fun showToast(resId: Int) {
    _toast.tryEmit(ToastEffect(resId))
  }
}