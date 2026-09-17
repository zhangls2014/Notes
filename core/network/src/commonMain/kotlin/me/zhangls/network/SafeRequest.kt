package me.zhangls.network

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse


suspend inline fun <reified T> safeRequest(
  crossinline block: suspend () -> HttpResponse
): NetworkResult<T> {
  return try {
    val result: ApiResponse<T> = block().body()
    if (result.code == 0 && result.data != null) {
      NetworkResult.Success(result.data)
    } else {
      val business = NetworkError.Business(code = result.code, message = result.message)
      NetworkResult.Failure(business)
    }
  } catch (e: ClientRequestException) {
    NetworkResult.Failure(
      NetworkError.Http(
        code = e.response.status.value,
        message = e.message
      )
    )
  } catch (e: ServerResponseException) {
    NetworkResult.Failure(
      NetworkError.Http(
        code = e.response.status.value,
        message = e.message
      )
    )
  } catch (e: RedirectResponseException) {
    NetworkResult.Failure(
      NetworkError.Http(
        code = e.response.status.value,
        message = e.message
      )
    )
  } catch (e: Exception) {
    NetworkResult.Failure(NetworkError.Unknown(e))
  }
}