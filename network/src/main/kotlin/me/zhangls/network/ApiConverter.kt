package me.zhangls.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * @author zhangls
 */
fun <T> networkFlow(
  block: suspend () -> NetworkResult<T>
): Flow<NetworkResult<T>> = flow {
  emit(block())
}

suspend inline fun <T> safeApiCall(
  crossinline block: suspend () -> ApiResponse<T>
): NetworkResult<T> {
  return try {
    val response = block()

    when (response.code) {
      0 -> NetworkResult.Success(response.data)
      401 -> NetworkResult.Failure(NetworkError.Unauthorized)
      403 -> NetworkResult.Failure(NetworkError.TokenExpired)
      else -> NetworkResult.Failure(NetworkError.Business(code = response.code, message = response.message))
    }
  } catch (throwable: Throwable) {
    NetworkResult.Failure(mapThrowable(throwable))
  }
}

fun mapThrowable(throwable: Throwable): NetworkError = when (throwable) {
  is SocketTimeoutException -> NetworkError.Timeout
  is UnknownHostException -> NetworkError.NoConnection
  is HttpException -> when (throwable.code()) {
    401 -> NetworkError.Unauthorized
    403 -> NetworkError.TokenExpired
    else -> NetworkError.Http(code = throwable.code(), message = throwable.message())
  }
  else -> NetworkError.Unknown(throwable)
}