package me.zhangls.data.model

import kotlinx.serialization.Serializable

/** 登录凭据，只在数据和网络边界使用，不放入 UI State。 */
@Serializable
class AuthTokens(val accessToken: String, val refreshToken: String = "") {
  override fun toString(): String = "AuthTokens([REDACTED])"
}
