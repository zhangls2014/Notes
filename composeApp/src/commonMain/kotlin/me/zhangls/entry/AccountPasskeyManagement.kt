package me.zhangls.entry

import me.zhangls.login.api.PasskeyProvider
import me.zhangls.profile.api.PasskeyManagement
import me.zhangls.profile.api.PasskeySessionVerificationRequiredException
import me.zhangls.profile.api.RegisteredPasskey

internal class AccountPasskeyManagement(
  private val auth: PasswordlessAuthClient,
  private val provider: PasskeyProvider?,
) : PasskeyManagement {
  override val canAdd: Boolean get() = provider != null

  override suspend fun list(): List<RegisteredPasskey> = withSession { auth.listPasskeys() }

  override suspend fun add(): Boolean = withSession { auth.addPasskey(checkNotNull(provider)) }

  override suspend fun revoke(id: String) = withSession { auth.revokePasskey(id) }

  private suspend fun <T> withSession(operation: suspend () -> T): T {
    return try {
      operation()
    } catch (failure: AuthHttpException) {
      if (failure.status == 403 || failure.code == "invalid_session") {
        throw PasskeySessionVerificationRequiredException()
      }
      throw failure
    }
  }
}
