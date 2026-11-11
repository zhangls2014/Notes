package me.zhangls.entry.data

import me.zhangls.data.repository.UserRepository
import me.zhangls.entry.configuredAuthEndpoint
import me.zhangls.network.TokenPair
import me.zhangls.network.TokenProvider
import org.koin.core.annotation.Singleton

@Singleton(binds = [TokenProvider::class])
class TokenProviderImpl(private val user: UserRepository) : TokenProvider {
  override suspend fun getTokens(): TokenPair? {
    if (configuredAuthEndpoint != null) return null
    return user.getTokens()?.takeIf { it.accessToken == "mock-access-token" && it.refreshToken.isEmpty() }
      ?.let { TokenPair(it.accessToken, it.refreshToken) }
  }

  override suspend fun clear() {
    user.clear()
  }
}
