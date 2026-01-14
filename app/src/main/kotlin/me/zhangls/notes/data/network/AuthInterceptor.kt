package me.zhangls.notes.data.network

import me.zhangls.network.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response

/**
 * @author zhangls
 */
class AuthInterceptor(private val tokenProvider: TokenProvider) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val token = tokenProvider.getAccessToken()

    val request = chain.request().newBuilder()
      .apply {
        // 自动添加 Token 到请求头
        if (token != null) addHeader("Authorization", "Bearer $token")
      }
      .build()

    return chain.proceed(request)
  }
}