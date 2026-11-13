package me.zhangls.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp

actual fun httpClient(
  retryOnConnectionFailure: Boolean,
  config: HttpClientConfig<*>.() -> Unit,
) = HttpClient(OkHttp) {
  config(this)

  engine {
    config {
      retryOnConnectionFailure(retryOnConnectionFailure)
    }
  }
}
