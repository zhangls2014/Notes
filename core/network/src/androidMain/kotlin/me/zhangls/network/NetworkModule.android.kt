package me.zhangls.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.annotation.Factory


@Factory
actual fun httpClient(
  config: HttpClientConfig<*>.() -> Unit
) = HttpClient(OkHttp) {
  config(this)

  engine {
    config {
      retryOnConnectionFailure(true)
    }
  }
}
