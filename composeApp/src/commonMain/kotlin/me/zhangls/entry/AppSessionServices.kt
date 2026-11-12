package me.zhangls.entry

import me.zhangls.data.repository.AccountDataRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.profile.api.AccountProfileRemote
import org.koin.core.annotation.Singleton

@Singleton(binds = [AccountProfileRemote::class])
internal class AppSessionServices(
  val accounts: AccountDataRepository,
  users: UserRepository,
) : AccountProfileRemote {
  val auth = configuredAuthEndpoint()?.let { PasswordlessAuthClient(it, users) }
  val passkeysEnabled = auth != null && BuildKonfig.AUTH_PASSKEYS_ENABLED

  override suspend fun updateUsername(userId: String, username: String) = checkNotNull(auth).updateUsername(userId, username)

  override suspend fun sendEmailChangeCode(userId: String, email: String, password: String) =
    checkNotNull(auth).sendEmailChangeCode(userId, email, password)

  override suspend fun changeEmail(userId: String, email: String, code: String) =
    checkNotNull(auth).changeEmail(userId, email, code)

  override suspend fun getProfile(userId: String) = checkNotNull(auth).getProfile(userId)

  override suspend fun getAvatar(userId: String) = checkNotNull(auth).getAvatar(userId)

  override suspend fun updateAvatar(userId: String, jpeg: ByteArray) = checkNotNull(auth).updateAvatar(userId, jpeg)
}
