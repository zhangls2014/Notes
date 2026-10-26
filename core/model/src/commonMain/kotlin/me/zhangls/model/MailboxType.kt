package me.zhangls.model

private const val SPAM_MAILBOX_CODE = 3
private const val TRASH_MAILBOX_CODE = 4

/**
 * 邮箱分类。
 *
 * 跨层共享类型：`core:data` 的读模型 `EmailModel.mailbox` 使用它，`core:database`
 * 的实体 `EmailEntity.mailbox`（经 `Converters` 落盘为 Int）也使用它。
 * 而 `core:data` 与 `core:database` 之间不能互相依赖（前者依赖后者），
 * 故与 [AppLanguage] / [DarkThemeConfig] 一样下沉到 core:model。
 *
 * @author zhangls
 */
enum class MailboxType(val value: Int) {
  INBOX(0),
  DRAFTS(1),
  SENT(2),
  SPAM(SPAM_MAILBOX_CODE),
  TRASH(TRASH_MAILBOX_CODE),
}
