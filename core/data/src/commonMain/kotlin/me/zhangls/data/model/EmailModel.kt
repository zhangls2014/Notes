package me.zhangls.data.model

import me.zhangls.data.database.entity.EmailEntity
import me.zhangls.data.database.entity.RecipientIdsCodec
import me.zhangls.data.type.MailboxType

data class EmailModel(
  val id: Long,
  val sender: AccountModel,
  val recipients: List<AccountModel> = emptyList(),
  val subject: String,
  val body: String,
  val isImportant: Boolean = false,
  val isStarred: Boolean = false,
  val mailbox: MailboxType = MailboxType.INBOX,
  val createdAt: String,
  val threads: List<EmailModel> = emptyList(),
)


fun EmailModel.toEntity(parentEmailId: Long?): EmailEntity {
  return EmailEntity(
    id = id,
    senderId = sender.id,
    recipientIds = RecipientIdsCodec.encode(recipients.map { it.id }.toSet()),
    subject = subject,
    body = body,
    isImportant = isImportant,
    isStarred = isStarred,
    mailbox = mailbox,
    createdAt = createdAt,
    parentEmailId = parentEmailId
  )
}
