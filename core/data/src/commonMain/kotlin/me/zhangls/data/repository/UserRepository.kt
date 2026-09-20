package me.zhangls.data.repository

import kotlinx.coroutines.flow.Flow
import me.zhangls.data.model.UserModel

interface UserRepository {
  val userFlow: Flow<UserModel?>

  suspend fun getUser(): UserModel?

  suspend fun update(user: UserModel)

  suspend fun updateAvatar(avatar: String)

  suspend fun updateEmailSearchHistory(keyword: String)

  suspend fun deleteEmailSearchHistory(keyword: String)

  suspend fun clear()
}
