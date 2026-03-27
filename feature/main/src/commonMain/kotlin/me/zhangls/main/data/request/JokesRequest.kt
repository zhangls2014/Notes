package me.zhangls.main.data.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import me.zhangls.main.data.response.JokePageResponse
import me.zhangls.network.safeRequest
import org.koin.core.annotation.Factory


@Factory
class JokesRequest(val client: HttpClient) {
  suspend fun jokes(
    appId: String,
    appSecret: String,
    page: Int
  ) = safeRequest<JokePageResponse> {
    client.get("api/jokes/list") {
      parameter("app_id", appId)
      parameter("app_secret", appSecret)
      parameter("page", page)
    }
  }
}