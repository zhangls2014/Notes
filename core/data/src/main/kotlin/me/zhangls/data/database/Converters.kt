@file:Suppress("unused")

package me.zhangls.data.database

import androidx.room3.TypeConverter
import me.zhangls.data.type.MailboxType

class Converters {
  @TypeConverter
  fun mailboxTypeToInt(type: MailboxType): Int = type.value

  @TypeConverter
  fun intToMailboxType(value: Int): MailboxType {
    return MailboxType.entries.find { it.value == value } ?: MailboxType.INBOX
  }
}