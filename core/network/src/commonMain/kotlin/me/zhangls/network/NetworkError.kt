package me.zhangls.network

/**
 * @author zhangls
 */
sealed interface NetworkError {
  // HTTP 层
  data class Http(val code: Int, val message: String) : NetworkError

  // 业务层（后端返回 code != success）
  data class Business(val code: Int, val message: String) : NetworkError

  // 网络异常
  object Timeout : NetworkError
  object NoConnection : NetworkError

  // 鉴权相关
  object Unauthorized : NetworkError
  object TokenExpired : NetworkError

  // 其他
  data class Unknown(val throwable: Throwable) : NetworkError
}