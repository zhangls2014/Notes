package me.zhangls.database.entity

import kotlin.test.Test
import kotlin.test.assertEquals

class RecipientIdsCodecTest {

  @Test
  fun `encode returns sorted unique json array`() {
    assertEquals("[4,9]", RecipientIdsCodec.encode(setOf(9L, 4L, 9L)))
  }

  @Test
  fun `decode returns empty set for blank string`() {
    assertEquals(emptySet(), RecipientIdsCodec.decode(""))
  }

  @Test
  fun `decode returns ids from json array`() {
    assertEquals(setOf(4L, 9L), RecipientIdsCodec.decode("[4,9]"))
  }
}
