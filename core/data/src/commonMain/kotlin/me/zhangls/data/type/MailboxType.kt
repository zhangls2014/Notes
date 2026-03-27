package me.zhangls.data.type

enum class MailboxType(val value: Int) {
  INBOX(0),
  DRAFTS(1),
  SENT(2),
  SPAM(3),
  TRASH(4),
}