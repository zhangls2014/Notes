package me.zhangls.data.model

import me.zhangls.data.database.entity.AccountEntity

data class AccountModel(
  val id: Long,
  val firstName: String,
  val lastName: String,
  val email: String,
  val altEmail: String,
  val avatar: Int,
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