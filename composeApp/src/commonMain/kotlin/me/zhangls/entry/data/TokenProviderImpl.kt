package me.zhangls.entry.data

import me.zhangls.data.repository.UserRepository
import me.zhangls.network.TokenProvider
import org.koin.core.annotation.Singleton

/**
 * @author zhangls
 */
@Singleton
class TokenProviderImpl(private val user: UserRepository) : TokenProvider {
  override suspend fun refreshToken(): String? {
    return user.getUser()?.refreshToken
  }

  override suspend fun getAccessToken(): String? {
    return user.getUser()?.accessToken
  }

  override suspend fun clear() {
  }
}