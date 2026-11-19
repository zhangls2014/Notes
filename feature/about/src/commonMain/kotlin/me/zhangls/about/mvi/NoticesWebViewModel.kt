package me.zhangls.about.mvi

import androidx.lifecycle.ViewModel
import me.zhangls.about.NoticesWebSession

internal class NoticesWebViewModel : ViewModel() {
  private var url: String? = null
  private var current: NoticesWebSession? = null

  fun session(url: String): NoticesWebSession {
    if (this.url != url) {
      current?.release()
      current = NoticesWebSession(url)
      this.url = url
    }
    return checkNotNull(current)
  }

  override fun onCleared() {
    current?.release()
  }
}
