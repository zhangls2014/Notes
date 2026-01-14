package me.zhangls.notes.data.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * @author zhangls
 */
class TokenAuthenticatorImpl @Inject constructor(
  private val tokenProvider: TokenProviderImpl,
  private val networkGlobalNotifier: NetworkGlobalNotifier
) : Authenticator {
  override fun authenticate(route: Route?, response: Response): Request? {
    val headerKey = when (response.code) {
      401 -> "Authorization"
      407 -> "Proxy-Authorization"
      else -> return null
    }

    synchronized(this) {
      if (tokenProvider.getAccessToken() == null) {
        networkGlobalNotifier.notifyUnauthorized()
        return null
      }

      val newToken = tokenProvider.refreshToken()
      if (newToken == null) {
        networkGlobalNotifier.notifyTokenExpired()
        return null
      }

      return response.request.newBuilder()
        .header(headerKey, "Bearer $newToken")
        .build()
    }
  }
}