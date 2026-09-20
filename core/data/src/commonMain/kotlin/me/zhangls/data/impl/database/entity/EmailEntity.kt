package me.zhangls.data.impl.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import me.zhangls.data.type.MailboxType

@Entity(
  tableName = "email",
  indices = [
    Index("senderId"),
    Index("parentEmailId")
  ]
)
data class EmailEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long,
  val senderId: Long,
  @ColumnInfo(defaultValue = "'[]'")
  val recipientIds: String = "[]",
  val subject: String,
  val body: String,
  val isImportant: Boolean = false,
  val isStarred: Boolean = false,
  val mailbox: MailboxType = MailboxType.INBOX,
  val createdAt: String,
  // 用于 threads（自关联）
  val parentEmailId: Long? = null
)
