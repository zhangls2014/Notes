package me.zhangls.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

actual fun httpClient(
  // URLSession has no equivalent to OkHttp retryOnConnectionFailure.
  @Suppress("UNUSED_PARAMETER") retryOnConnectionFailure: Boolean,
  config: HttpClientConfig<*>.() -> Unit,
) = HttpClient(Darwin) {
  config(this)
  engine {
    configureRequest {
      setAllowsCellularAccess(true)
    }
  }
}
