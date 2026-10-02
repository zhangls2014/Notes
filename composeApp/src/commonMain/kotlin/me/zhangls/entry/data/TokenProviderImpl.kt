package me.zhangls.entry.data

import me.zhangls.data.repository.UserRepository
import me.zhangls.network.TokenPair
import me.zhangls.network.TokenProvider
import org.koin.core.annotation.Singleton

@Singleton(binds = [TokenProvider::class])
class TokenProviderImpl(private val user: UserRepository) : TokenProvider {
  override suspend fun getTokens(): TokenPair? = user.getTokens()?.let {
    TokenPair(it.accessToken, it.refreshToken)
  }

  override suspend fun clear() {
    user.clear()
  }
}
