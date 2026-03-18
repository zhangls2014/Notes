package me.zhangls.network


import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Qualifier
import org.koin.core.annotation.Singleton
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * @author zhangls
 */
@Module
@ComponentScan("me.zhangls.network")
class NetworkModule

@Factory
@Named("AuthInterceptor")
fun provideAuthInterceptor(tokenProvider: TokenProviderImpl): Interceptor = AuthInterceptor(tokenProvider)

@Singleton
@Named("HttpLoggingInterceptor")
fun provideLogInterceptor(@Provided config: NetworkConfig): Interceptor = HttpLoggingInterceptor().apply {
  level = if (config.isDebug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
}

@Singleton
fun provideOkHttpClient(
  @NetworkTimeout timeout: Int,
  interceptors: List<Interceptor>,
  tokenAuthenticator: TokenAuthenticatorImpl,
): OkHttpClient = OkHttpClient.Builder()
  .connectTimeout(timeout.toLong(), TimeUnit.SECONDS)
  .readTimeout(timeout.toLong(), TimeUnit.SECONDS)
  .also { builder ->
    interceptors.forEach { builder.addInterceptor(it) }
  }
  .authenticator(tokenAuthenticator)
  .build()

@Singleton
fun provideRetrofit(
  @Provided config: NetworkConfig,
  client: OkHttpClient,
  @NetworkJson json: Json
): Retrofit = Retrofit.Builder()
  .baseUrl(config.baseUrl)
  .client(client)
  .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
  .build()

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NetworkJson

@Singleton
@NetworkJson
fun provideJson(): Json = Json {
  ignoreUnknownKeys = true
  explicitNulls = false
  encodeDefaults = true
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NetworkTimeout

@Factory
@NetworkTimeout
fun provideNetworkTimeout(): Int = 30
