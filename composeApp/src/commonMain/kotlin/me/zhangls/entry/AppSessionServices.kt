package me.zhangls.entry

import io.ktor.client.HttpClient
import kotlinx.io.IOException
import kotlinx.serialization.json.JsonObject
import me.zhangls.data.repository.MailRemote
import me.zhangls.data.repository.MailRequestException
import me.zhangls.data.repository.UserRepository
import me.zhangls.profile.api.AccountProfileRemote
import org.koin.core.annotation.Singleton

@Singleton(binds = [AccountProfileRemote::class, MailRemote::class])
internal class AppSessionServices(
  users: UserRepository,
  client: HttpClient,
  config: AuthenticationConfig,
) : AccountProfileRemote, MailRemote {
  val auth = config.origin?.let { PasswordlessAuthClient(it, users, client) }
  val passkeysEnabled = auth != null && config.passkeysEnabled

  override suspend fun request(accountId: String, input: JsonObject): String = try {
    val authenticated = auth ?: throw MailRequestException(IllegalStateException("Mail service is not configured"))
    authenticated.mail(accountId, input)
  } catch (failure: AuthHttpException) {
    throw MailRequestException(failure, failure.status)
  } catch (failure: IOException) {
    throw MailRequestException(failure)
  }

  override suspend fun updateUsername(userId: String, username: String) = checkNotNull(auth).updateUsername(userId, username)

  override suspend fun verifyEmailChangePassword(userId: String, password: String): Boolean =
    checkNotNull(auth).verifyEmailChangePassword(userId, password)

  override suspend fun sendEmailChangeCode(userId: String, email: String, password: String) =
    checkNotNull(auth).sendEmailChangeCode(userId, email, password)

  override suspend fun changeEmail(userId: String, email: String, code: String) =
    checkNotNull(auth).changeEmail(userId, email, code)

  override suspend fun getProfile(userId: String) = checkNotNull(auth).getProfile(userId)

  override suspend fun getAvatar(userId: String) = checkNotNull(auth).getAvatar(userId)

  override suspend fun updateAvatar(userId: String, jpeg: ByteArray) = checkNotNull(auth).updateAvatar(userId, jpeg)
}
