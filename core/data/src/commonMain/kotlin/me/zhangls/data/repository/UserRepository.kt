package me.zhangls.data.repository

import kotlinx.coroutines.flow.Flow
import me.zhangls.data.model.AuthTokens
import me.zhangls.data.model.UserModel

interface UserRepository {
  val userFlow: Flow<UserModel?>

  suspend fun getUser(): UserModel?

  suspend fun login(user: UserModel, tokens: AuthTokens)

  suspend fun getTokens(): AuthTokens?

  /** Rotate credentials without rewriting the profile or resetting its device lock. */
  suspend fun replaceTokens(userId: String, expectedRefreshToken: String, tokens: AuthTokens): Boolean

  /** 返回 false 表示登录用户或头像已变化；不覆盖其他账户或较新的头像。 */
  suspend fun updateAvatar(avatar: String, expectedUser: UserModel): Boolean

  /** Apply server fields only while the expected account and cached profile still match. */
  suspend fun updateAccountProfile(
    account: String,
    avatar: String?,
    revision: String?,
    expectedUser: UserModel,
    email: String? = expectedUser.email,
  ): Boolean

  /** 当前会话存在且写入成功时返回 true。 */
  suspend fun updateBiometricUnlock(enabled: Boolean): Boolean

  suspend fun updateEmailSearchHistory(keyword: String)

  suspend fun deleteEmailSearchHistory(keyword: String)

  suspend fun clear()
}
