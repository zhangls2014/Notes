package me.zhangls.profile.api

data class AccountProfile(val id: String, val account: String, val avatarRevision: String?, val email: String? = null)

class AccountAvatar(val revision: String, val jpeg: ByteArray)

interface AccountProfileRemote {
  suspend fun updateUsername(userId: String, username: String)
  suspend fun sendEmailChangeCode(userId: String, email: String, password: String)
  suspend fun changeEmail(userId: String, email: String, code: String)
  suspend fun getProfile(userId: String): AccountProfile
  suspend fun getAvatar(userId: String): AccountAvatar
  suspend fun updateAvatar(userId: String, jpeg: ByteArray): AccountAvatar
}

interface AccountProfileRefresh {
  suspend fun refresh()
}
