package me.zhangls.notes.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap
import me.zhangls.network.NetworkModule
import me.zhangls.notes.BuildConfig
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
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
  @IntKey(2)
  fun provideLogInterceptor(): Interceptor = HttpLoggingInterceptor().apply {
    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
  }

  @Provides
  @Singleton
  @NetworkModule.BaseUrl
  fun provideBaseUrl(): String = BuildConfig.BASE_URL
}