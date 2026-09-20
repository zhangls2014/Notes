@file:Suppress("unused")

package me.zhangls.database

import androidx.room3.ColumnTypeConverter
import me.zhangls.model.MailboxType

class Converters {
  @ColumnTypeConverter
  fun mailboxTypeToInt(type: MailboxType): Int = type.value

  @ColumnTypeConverter
  fun intToMailboxType(value: Int): MailboxType {
    return MailboxType.entries.find { it.value == value } ?: MailboxType.INBOX
  }
}
