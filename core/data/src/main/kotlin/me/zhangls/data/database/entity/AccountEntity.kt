package me.zhangls.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account")
data class AccountEntity(
  @PrimaryKey
  val id: Long,
  val firstName: String,
  val lastName: String,
  val email: String,
  val altEmail: String,
  val avatar: Int,
)