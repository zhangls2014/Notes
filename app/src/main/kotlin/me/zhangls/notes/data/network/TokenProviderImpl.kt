package me.zhangls.notes.data.network

import me.zhangls.network.TokenProvider
import javax.inject.Inject

/**
 * @author zhangls
 */
class TokenProviderImpl @Inject constructor() : TokenProvider {
  override fun refreshToken(): String? = null

  override fun getAccessToken(): String? = null

  override fun clear() {
  }
}