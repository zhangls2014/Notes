package me.zhangls.data.model

/**
 * 邮件写模型：创建一封草稿所需的全部信息。
 *
 * 与 [EmailModel] 的区别在于它只表达"要写入什么"：
 * - 发件账户由数据层取默认账户，调用方不必传；
 * - [recipientIds] 是收件人 ID 集合，落盘编码（JSON 数组字符串）由数据层内部完成。
 */
data class EmailDraft(
  val recipientIds: Set<Long>,
  val subject: String,
  val body: String,
  val createdAt: String,
)
