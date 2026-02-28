package me.zhangls.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * @author zhangls
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
  @Provides
  @Singleton
  @IntoMap
  @IntKey(1)
  fun provideAuthInterceptor(tokenProvider: TokenProviderImpl): Interceptor = AuthInterceptor(tokenProvider)

  @Provides
  @Singleton
  fun provideOkHttpClient(
    @NetworkTimeout timeout: Int,
    interceptors: Map<Int, @JvmSuppressWildcards Interceptor>,
    tokenAuthenticator: TokenAuthenticatorImpl,
  ): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(timeout.toLong(), TimeUnit.SECONDS)
    .readTimeout(timeout.toLong(), TimeUnit.SECONDS)
    .also { builder ->
      interceptors
        .toSortedMap()
        .values
        .forEach { builder.addInterceptor(it) }
    }
    .authenticator(tokenAuthenticator)
    .build()

  @Provides
  @Singleton
  @NetworkJson
  fun provideJson(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
  }

  @Qualifier
  @Retention(AnnotationRetention.BINARY)
  annotation class NetworkJson

  @Provides
  @Singleton
  fun provideRetrofit(
    @BaseUrl baseUrl: String,
    client: OkHttpClient,
    @NetworkJson json: Json
  ): Retrofit = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(client)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()

  @Qualifier
  @Retention(AnnotationRetention.BINARY)
  annotation class NetworkTimeout

  @Provides
  @Singleton
  @NetworkTimeout
  fun provideNetworkTimeout(): Int = 30

  @Qualifier
  @Retention(AnnotationRetention.BINARY)
  annotation class BaseUrl
}