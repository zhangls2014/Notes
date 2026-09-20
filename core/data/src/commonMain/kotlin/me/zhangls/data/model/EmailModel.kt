package me.zhangls.data.model

import me.zhangls.model.MailboxType

/**
 * 邮件对外模型。
 *
 * 注意：[recipients] 与 [threads] 只在写入路径（`EmailsRepository.insertEmails`）被填充；
 * 读取路径（列表 / 详情 / 搜索）不会查询它们，取到的值为空。
 */
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
