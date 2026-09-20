package me.zhangls.database.entity

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * 收件人 ID 的落盘编码（JSON 数组字符串）。
 *
 * 属于存储细节，不得外泄到调用方；模块边界（`implementation` 依赖）保证 `feature` 层看不到它。
 */
object RecipientIdsCodec {
  private val json = Json
  private val serializer = ListSerializer(Long.serializer())

  fun encode(ids: Set<Long>): String {
    return json.encodeToString(serializer, ids.toList().distinct().sorted())
  }

  fun decode(raw: String): Set<Long> {
    return if (raw.isBlank()) emptySet() else json.decodeFromString(serializer, raw).toSet()
  }
}
