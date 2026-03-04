package me.zhangls.data.model

import me.zhangls.data.database.entity.EmailEntity
import me.zhangls.data.type.MailboxType

data class EmailModel(
  val id: Long,
  val sender: AccountModel,
  val recipients: List<AccountModel> = emptyList(),
  val subject: String,
  val body: String,
  var isImportant: Boolean = false,
  var isStarred: Boolean = false,
  var mailbox: MailboxType = MailboxType.INBOX,
  val createdAt: String,
  val threads: List<EmailModel> = emptyList(),
)


fun EmailModel.toEntity(parentEmailId: Long?): EmailEntity {
  return EmailEntity(
    id = id,
    senderId = sender.id,
    subject = subject,
    body = body,
    isImportant = isImportant,
    isStarred = isStarred,
    mailbox = mailbox,
    createdAt = createdAt,
    parentEmailId = parentEmailId
  )
}