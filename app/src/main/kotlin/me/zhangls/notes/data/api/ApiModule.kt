package me.zhangls.notes.data.api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

/**
 * @author zhangls
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
  @Provides
  fun provideJokesApi(retrofit: Retrofit): JokesApi = retrofit.create(JokesApi::class.java)
}