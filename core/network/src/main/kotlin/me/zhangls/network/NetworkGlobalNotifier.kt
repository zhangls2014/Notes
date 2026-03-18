package me.zhangls.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.koin.core.annotation.Singleton

/**
 * 网络请求全局通知器
 *
 * @author zhangls
 */
@Singleton
class NetworkGlobalNotifier {
  private val _events = MutableSharedFlow<NetworkError>(replay = 0, extraBufferCapacity = 1)
  val events = _events.asSharedFlow()


  fun notifyNoConnection() {
    emit(NetworkError.NoConnection)
  }

  fun notifyTokenExpired() {
    emit(NetworkError.TokenExpired)
  }

  fun notifyUnauthorized() {
    emit(NetworkError.Unauthorized)
  }

  private fun emit(error: NetworkError) {
    _events.tryEmit(error)
  }
}