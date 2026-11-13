package me.zhangls.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

private const val HTTP_TIMEOUT_MILLIS = 15_000L

/**
 * @author zhangls
 */
@Module
@ComponentScan("me.zhangls.network")
class NetworkModule

expect fun httpClient(
  retryOnConnectionFailure: Boolean = true,
  config: HttpClientConfig<*>.() -> Unit = {},
): HttpClient

@Singleton
fun provideKtorClient(): HttpClient = httpClient(retryOnConnectionFailure = false) {
  expectSuccess = false
  followRedirects = false
  install(HttpTimeout) { requestTimeoutMillis = HTTP_TIMEOUT_MILLIS }
}
