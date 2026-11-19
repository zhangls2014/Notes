package me.zhangls.data.model

/** 邮件发送内容。operationId 在同一次发送重试时保持不变。 */
data class EmailDraft(
  val recipientIds: Set<Long>,
  val subject: String,
  val body: String,
  val createdAt: String,
  val operationId: String = "",
  val mode: String = "new",
  val sourceId: Long? = null,
)
