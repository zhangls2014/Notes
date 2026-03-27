package me.zhangls.network

/**
 * @author zhangls
 */
interface TokenProvider {
  suspend fun refreshToken(): String?
  suspend fun getAccessToken(): String?
  suspend fun clear()
}