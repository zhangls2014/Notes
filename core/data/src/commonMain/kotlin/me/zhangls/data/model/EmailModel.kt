package me.zhangls.data.model

import kotlinx.serialization.Serializable
import me.zhangls.model.MailboxType

/** 邮件读模型；服务端返回完整的发件人、收件人与 UTC 时间戳。 */
@Serializable
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
