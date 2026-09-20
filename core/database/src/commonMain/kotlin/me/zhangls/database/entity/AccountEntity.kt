package me.zhangls.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "account")
data class AccountEntity(
  @PrimaryKey
  val id: Long,
  val firstName: String,
  val lastName: String,
  val email: String,
  val altEmail: String,
  val avatar: String,
  // 是否默认账户（发件账户）；未显式设置时按 id 兜底
  @ColumnInfo(defaultValue = "0")
  val isDefault: Boolean = false,
)