package me.zhangls.network

/** 一次读取整组凭据，避免并发切换账号时混用不同会话的 Token。 */
class TokenPair(val accessToken: String, val refreshToken: String) {
  override fun toString(): String = "TokenPair([REDACTED])"
}

interface TokenProvider {
  suspend fun getTokens(): TokenPair?
  suspend fun clear()
}
