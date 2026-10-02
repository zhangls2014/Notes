package me.zhangls.data.impl.security

import kotlinx.cinterop.*
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Singleton
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

@OptIn(ExperimentalForeignApi::class)
@Singleton(binds = [SecureTokenStore::class])
internal class IosSecureTokenStore : SecureTokenStore by KeychainCredentialsStore("me.zhangls.notes.auth.v1")

@OptIn(ExperimentalForeignApi::class)
internal class KeychainCredentialsStore(private val service: String) : SecureTokenStore {
  private fun query(): Map<Any?, Any?> = mapOf(
    CFBridgingRelease(kSecClass) to CFBridgingRelease(kSecClassGenericPassword),
    CFBridgingRelease(kSecAttrService) to service,
    CFBridgingRelease(kSecAttrAccount) to "current-session",
    CFBridgingRelease(kSecAttrSynchronizable) to CFBridgingRelease(kCFBooleanFalse),
  )

  override suspend fun read(): StoredCredentials? = memScoped {
    val result = alloc<CFTypeRefVar>()
    result.value = null
    val dictionary = CFBridgingRetain(query() + mapOf(CFBridgingRelease(kSecReturnData) to CFBridgingRelease(kCFBooleanTrue), CFBridgingRelease(kSecMatchLimit) to CFBridgingRelease(kSecMatchLimitOne)))
    try {
      val status = SecItemCopyMatching(dictionary as CFDictionaryRef?, result.ptr)
      if (status == errSecItemNotFound) return@memScoped null
      check(status == errSecSuccess) { "Keychain read failed ($status)" }
      val data = CFBridgingRelease(result.value) as NSData
      val text = NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
      checkNotNull(text) { "Invalid Keychain credential encoding" }
      Json.decodeFromString<StoredCredentials>(text)
    } finally { CFRelease(dictionary) }
  }

  override suspend fun write(credentials: StoredCredentials) {
    val bytes = Json.encodeToString(credentials).encodeToByteArray()
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    val dictionary = CFBridgingRetain(query())
    val attributes = CFBridgingRetain(mapOf(
      CFBridgingRelease(kSecValueData) to data,
      CFBridgingRelease(kSecAttrAccessible) to CFBridgingRelease(kSecAttrAccessibleWhenUnlockedThisDeviceOnly),
    ))
    try {
      val status = SecItemUpdate(dictionary as CFDictionaryRef?, attributes as CFDictionaryRef?)
      if (status == errSecItemNotFound) {
        val addition = CFBridgingRetain(query() + mapOf(
          CFBridgingRelease(kSecValueData) to data,
          CFBridgingRelease(kSecAttrAccessible) to CFBridgingRelease(kSecAttrAccessibleWhenUnlockedThisDeviceOnly),
        ))
        try { check(SecItemAdd(addition as CFDictionaryRef?, null) == errSecSuccess) { "Keychain insert failed" } }
        finally { CFRelease(addition) }
      } else check(status == errSecSuccess) { "Keychain update failed ($status)" }
    } finally { CFRelease(attributes); CFRelease(dictionary) }
  }

  override suspend fun clear() {
    val dictionary = CFBridgingRetain(query())
    try {
      val status = SecItemDelete(dictionary as CFDictionaryRef?)
      check(status == errSecSuccess || status == errSecItemNotFound) { "Keychain delete failed ($status)" }
    } finally { CFRelease(dictionary) }
  }
}
