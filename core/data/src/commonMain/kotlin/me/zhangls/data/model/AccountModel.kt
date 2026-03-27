package me.zhangls.data.model

import kotlinx.serialization.Serializable
import me.zhangls.data.database.entity.AccountEntity

@Serializable
data class AccountModel(
  val id: Long,
  val firstName: String,
  val lastName: String,
  val email: String,
  val altEmail: String,
  val avatar: String,
) {
  val fullName: String = "$firstName $lastName"
}

fun AccountModel.toEntity(): AccountEntity {
  return AccountEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    altEmail = altEmail,
    avatar = avatar
  )
}

fun AccountEntity.toDomain(): AccountModel {
  return AccountModel(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = email,
    altEmail = altEmail,
    avatar = avatar
  )
}