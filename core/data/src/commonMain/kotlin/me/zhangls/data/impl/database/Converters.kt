@file:Suppress("unused")

package me.zhangls.data.impl.database

import androidx.room3.ColumnTypeConverter
import me.zhangls.data.type.MailboxType

internal class Converters {
  @ColumnTypeConverter
  fun mailboxTypeToInt(type: MailboxType): Int = type.value

  @ColumnTypeConverter
  fun intToMailboxType(value: Int): MailboxType {
    return MailboxType.entries.find { it.value == value } ?: MailboxType.INBOX
  }
}
