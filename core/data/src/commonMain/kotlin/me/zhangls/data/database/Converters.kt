@file:Suppress("unused")

package me.zhangls.data.database

import androidx.room3.ColumnTypeConverter
import me.zhangls.data.type.MailboxType

class Converters {
  @ColumnTypeConverter
  fun mailboxTypeToInt(type: MailboxType): Int = type.value

  @ColumnTypeConverter
  fun intToMailboxType(value: Int): MailboxType {
    return MailboxType.entries.find { it.value == value } ?: MailboxType.INBOX
  }
}
