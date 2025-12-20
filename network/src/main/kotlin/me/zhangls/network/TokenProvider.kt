package me.zhangls.network

/**
 * @author zhangls
 */
interface TokenProvider {
  fun refreshToken(): String?
  fun getAccessToken(): String?
  fun clear()
}