package me.zhangls.network

/**
 * @author zhangls
 */
sealed interface NetworkResult<out T> {
  data class Success<T>(val data: T?) : NetworkResult<T>
  data class Failure(val error: NetworkError) : NetworkResult<Nothing>
}