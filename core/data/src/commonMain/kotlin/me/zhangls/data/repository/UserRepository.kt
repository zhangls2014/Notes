package me.zhangls.data.repository

import kotlinx.coroutines.flow.Flow
import me.zhangls.data.model.UserModel
import me.zhangls.data.model.AuthTokens

interface UserRepository {
  val userFlow: Flow<UserModel?>

  suspend fun getUser(): UserModel?

  suspend fun login(user: UserModel, tokens: AuthTokens)

  suspend fun getTokens(): AuthTokens?

  suspend fun updateAvatar(avatar: String)

  suspend fun updateEmailSearchHistory(keyword: String)

  suspend fun deleteEmailSearchHistory(keyword: String)

  suspend fun clear()
}
