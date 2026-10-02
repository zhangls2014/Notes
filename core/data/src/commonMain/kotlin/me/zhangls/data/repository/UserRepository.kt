package me.zhangls.data.repository

import kotlinx.coroutines.flow.Flow
import me.zhangls.data.model.UserModel
import me.zhangls.data.model.AuthTokens

interface UserRepository {
  val userFlow: Flow<UserModel?>

  suspend fun getUser(): UserModel?

  suspend fun login(user: UserModel, tokens: AuthTokens)

  suspend fun getTokens(): AuthTokens?

  /** 返回 false 表示登录用户或头像已变化；不覆盖其他账户或较新的头像。 */
  suspend fun updateAvatar(avatar: String, expectedUser: UserModel): Boolean

  suspend fun updateEmailSearchHistory(keyword: String)

  suspend fun deleteEmailSearchHistory(keyword: String)

  suspend fun clear()
}
