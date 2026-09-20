package me.zhangls.data.model

import kotlinx.serialization.Serializable

/**
 * 账户对外模型（发件人 / 收件人）。
 */
@Serializable
data class AccountModel(
  val id: Long,
  val firstName: String,
  val lastName: String,
  val email: String,
  val altEmail: String,
  val avatar: String,
  val isDefault: Boolean = false,
) {
  val fullName: String = "$firstName $lastName"
}