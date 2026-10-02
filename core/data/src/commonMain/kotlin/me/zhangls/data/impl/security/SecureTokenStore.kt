package me.zhangls.data.impl.security

import kotlinx.serialization.Serializable
import me.zhangls.data.model.AuthTokens

@Serializable
internal class StoredCredentials(val userId: String, val tokens: AuthTokens) {
  override fun toString(): String = "StoredCredentials([REDACTED])"
}

/** 实现必须原子更新整组凭据；暂时无法访问时抛错，不视为没有凭据。 */
internal interface SecureTokenStore {
  suspend fun read(): StoredCredentials?
  suspend fun write(credentials: StoredCredentials)
  suspend fun clear()
}
