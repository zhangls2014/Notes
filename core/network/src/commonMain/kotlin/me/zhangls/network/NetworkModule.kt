package me.zhangls.network


import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.Charsets
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.addDefaultResponseValidation
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.charsets.Charsets
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

/**
 * @author zhangls
 */
@Module
@ComponentScan("me.zhangls.network")
class NetworkModule

expect fun httpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient

@Singleton
fun provideKtorClient(networkConfig: NetworkConfig): HttpClient {
  return HttpClient {
    expectSuccess = true
    addDefaultResponseValidation()

    Charsets {
      register(Charsets.UTF_8)
    }

    install(HttpTimeout) {
      requestTimeoutMillis = 10_000
      connectTimeoutMillis = 10_000
    }

    install(DefaultRequest) {
      contentType(ContentType.Application.Json)
      url(networkConfig.baseUrl)
    }

    install(Logging) {
      logger = Logger.DEFAULT
      level = LogLevel.HEADERS
      sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }

    install(ContentNegotiation) {
      json(
        Json {
          ignoreUnknownKeys = true
          explicitNulls = false
          encodeDefaults = true
          isLenient = true
          prettyPrint = true
        }
      )
    }
  }
}
