package me.zhangls.profile.api

data class RegisteredPasskey(
  val id: String,
  val createdAt: String,
  val provider: String? = null,
  val deviceBrand: String? = null,
)

interface PasskeyManagement {
  val canAdd: Boolean

  suspend fun list(): List<RegisteredPasskey>
  suspend fun add(): Boolean
  suspend fun revoke(id: String)
}

class PasskeySessionVerificationRequiredException : Exception()
