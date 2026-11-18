@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package me.zhangls.about

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

internal actual fun webAssetDisk(): WebAssetDisk? = IosWebAssetDisk()

internal actual fun webAssetSha256(bytes: ByteArray): String {
  val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
  if (bytes.isEmpty()) {
    digest.usePinned { CC_SHA256(null, 0u, it.addressOf(0)) }
  } else {
    bytes.usePinned { source ->
      digest.usePinned { CC_SHA256(source.addressOf(0), bytes.size.toUInt(), it.addressOf(0)) }
    }
  }
  return digest.toByteArray().toHexString()
}

internal class IosWebAssetDisk : WebAssetDisk {
  private val manager = NSFileManager.defaultManager

  private val root: String?
    get() = manager.URLForDirectory(
      directory = NSCachesDirectory,
      inDomain = NSUserDomainMask,
      appropriateForURL = null,
      create = true,
      error = null,
    )?.path?.let { "$it/notes-web-assets" }

  private fun directory(asset: CachedWebAsset): String? = root?.let { "$it/${asset.directory}" }

  fun data(asset: CachedWebAsset): NSData? =
    directory(asset)?.let { NSData.dataWithContentsOfFile("$it/${asset.filename}") }

  override fun read(asset: CachedWebAsset): ByteArray? {
    val data = data(asset) ?: return null
    val bytes = ByteArray(data.length.toInt())
    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    return bytes
  }

  override fun write(asset: CachedWebAsset, bytes: ByteArray) {
    val directory = checkNotNull(directory(asset))
    check(manager.createDirectoryAtPath(directory, true, null, null))
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    check(data.writeToFile("$directory/${asset.filename}", atomically = true))
  }
}
