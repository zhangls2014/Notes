package me.zhangls.data.database.entity

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

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
