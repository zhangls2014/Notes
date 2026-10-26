package me.zhangls.database

import androidx.room3.ColumnTypeConverter
import me.zhangls.model.MailboxType

class Converters {
  // Called by Room-generated DAOs; the IDE cannot resolve those references reliably.
  @Suppress("unused")
  @ColumnTypeConverter
  fun mailboxTypeToInt(type: MailboxType): Int = type.value

  @Suppress("unused")
  @ColumnTypeConverter
  fun intToMailboxType(value: Int): MailboxType = MailboxType.entries.find { it.value == value } ?: MailboxType.INBOX
}
