package me.zhangls.data.impl.security

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.*
import platform.Security.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


@OptIn(ExperimentalForeignApi::class)
class KeychainQueryTest {
  @Test
  fun readQueryRequestsDataWithNativeCFBoolean() {
    val query = assertNotNull(keychainQuery("me.zhangls.notes.test.query", returnData = true))
    try {
      val value = assertNotNull(CFDictionaryGetValue(query, kSecReturnData))
      assertEquals(CFBooleanGetTypeID(), CFGetTypeID(value), "Security requires CFBoolean for kSecReturnData")
      assertEquals<Any?>(kCFBooleanTrue, value)
      assertEquals<Any?>(kSecMatchLimitOne, CFDictionaryGetValue(query, kSecMatchLimit))
    } finally { CFRelease(query) }
  }

  @Test
  fun queriesDisableSynchronizationWithNativeCFBoolean() {
    for (returnData in listOf(false, true)) {
      val query = assertNotNull(keychainQuery("me.zhangls.notes.test.query", returnData = returnData))
      try {
        val value = assertNotNull(CFDictionaryGetValue(query, kSecAttrSynchronizable))
        assertEquals(CFBooleanGetTypeID(), CFGetTypeID(value), "Security requires CFBoolean for kSecAttrSynchronizable")
        assertEquals<Any?>(kCFBooleanFalse, value)
      } finally { CFRelease(query) }
    }
  }
}
