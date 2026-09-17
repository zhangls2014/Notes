package me.zhangls.network

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse


suspend inline fun <reified T> safeRequest(
  crossinline block: suspend () -> HttpResponse
): NetworkResult<T> {
  return try {
    val result: ApiResponse<T> = block().body()
    if (result.code == 0) {
      // 业务成功时允许 data 为 null（如无返回体的操作型接口），
      // 与 Success(data: T?) 的可空设计保持一致
      NetworkResult.Success(result.data)
    } else {
      NetworkResult.Failure(
        NetworkError.Business(code = result.code, message = result.message)
      )
    }
  } catch (e: ResponseException) {
    // ClientRequestException / ServerResponseException / RedirectResponseException
    // 三个分支处理逻辑完全相同，统一由父类捕获
    NetworkResult.Failure(
      NetworkError.Http(
        code = e.response.status.value,
        message = e.message.orEmpty()
      )
    )
  } catch (e: Exception) {
    NetworkResult.Failure(NetworkError.Unknown(e))
  }
}
