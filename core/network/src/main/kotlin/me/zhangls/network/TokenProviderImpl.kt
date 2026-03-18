package me.zhangls.network

import org.koin.core.annotation.Factory


/**
 * @author zhangls
 */
@Factory
class TokenProviderImpl : TokenProvider {
  override fun refreshToken(): String? = null

  override fun getAccessToken(): String? = null

  override fun clear() {
  }
}