package me.zhangls.data.impl.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Singleton
import platform.CoreFoundation.CFDictionaryCreateMutableCopy
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecAttrSynchronizable
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
@Singleton(binds = [SecureTokenStore::class])
internal class IosSecureTokenStore : SecureTokenStore by KeychainCredentialsStore("me.zhangls.notes.auth.v1")

@OptIn(ExperimentalForeignApi::class)
internal class KeychainCredentialsStore(private val service: String) : SecureTokenStore {
  override suspend fun read(): StoredCredentials? = memScoped {
    val result = alloc<CFTypeRefVar>()
    result.value = null
    val dictionary = keychainQuery(service, returnData = true)
    try {
      val status = SecItemCopyMatching(dictionary as CFDictionaryRef?, result.ptr)
      if (status == errSecItemNotFound) return@memScoped null
      check(status == errSecSuccess) { "Keychain read failed ($status)" }
      val data = CFBridgingRelease(result.value) as NSData
      val text = NSString.create(data = data, encoding = NSUTF8StringEncoding) as String?
      checkNotNull(text) { "Invalid Keychain credential encoding" }
      Json.decodeFromString<StoredCredentials>(text)
    } finally {
      CFRelease(dictionary)
    }
  }

  override suspend fun write(credentials: StoredCredentials) {
    val bytes = Json.encodeToString(credentials).encodeToByteArray()
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    val dictionary = keychainQuery(service)
    val attributes = CFBridgingRetain(
      mapOf(
        CFBridgingRelease(kSecValueData) to data,
        CFBridgingRelease(kSecAttrAccessible) to CFBridgingRelease(kSecAttrAccessibleWhenUnlockedThisDeviceOnly),
      ),
    )
    try {
      val status = SecItemUpdate(dictionary as CFDictionaryRef?, attributes as CFDictionaryRef?)
      if (status == errSecItemNotFound) {
        val addition = keychainQuery(
          service,
          attributes = mapOf(
            CFBridgingRelease(kSecValueData) to data,
            CFBridgingRelease(kSecAttrAccessible) to CFBridgingRelease(kSecAttrAccessibleWhenUnlockedThisDeviceOnly),
          ),
        )
        try {
          check(SecItemAdd(addition as CFDictionaryRef?, null) == errSecSuccess) { "Keychain insert failed" }
        } finally {
          CFRelease(addition)
        }
      } else {
        check(status == errSecSuccess) { "Keychain update failed ($status)" }
      }
    } finally {
      CFRelease(attributes)
      CFRelease(dictionary)
    }
  }

  override suspend fun clear() {
    val dictionary = keychainQuery(service)
    try {
      val status = SecItemDelete(dictionary as CFDictionaryRef?)
      check(status == errSecSuccess || status == errSecItemNotFound) { "Keychain delete failed ($status)" }
    } finally {
      CFRelease(dictionary)
    }
  }
}

@OptIn(ExperimentalForeignApi::class)
internal fun keychainQuery(
  service: String,
  returnData: Boolean = false,
  attributes: Map<Any?, Any?> = emptyMap(),
): CFDictionaryRef {
  val bridged = CFBridgingRetain(
    mapOf(
      CFBridgingRelease(kSecClass) to CFBridgingRelease(kSecClassGenericPassword),
      CFBridgingRelease(kSecAttrService) to service,
      CFBridgingRelease(kSecAttrAccount) to "current-session",
    ) + attributes,
  )
  try {
    val dictionary = requireNotNull(CFDictionaryCreateMutableCopy(null, 0, bridged?.reinterpret()))
    // Kotlin Boolean round-trips as NSNumber, but Security requires native CFBoolean values.
    CFDictionarySetValue(dictionary, kSecAttrSynchronizable, kCFBooleanFalse)
    if (returnData) {
      CFDictionarySetValue(dictionary, kSecReturnData, kCFBooleanTrue)
      CFDictionarySetValue(dictionary, kSecMatchLimit, kSecMatchLimitOne)
    }
    return dictionary
  } finally {
    CFRelease(bridged)
  }
}
