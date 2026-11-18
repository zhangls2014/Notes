@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package me.zhangls.download

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import platform.CoreCrypto.CC_SHA256_CTX
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.CC_SHA256_Final
import platform.CoreCrypto.CC_SHA256_Init
import platform.CoreCrypto.CC_SHA256_Update
import platform.posix.truncate

internal actual fun truncateDownload(path: String, size: Long) {
  if (truncate(path, size) != 0) throw IOException("Unable to restore download checkpoint")
}

internal actual fun downloadSha256(path: String): String = memScoped {
  val context = alloc<CC_SHA256_CTX>()
  CC_SHA256_Init(context.ptr)
  val bytes = ByteArray(64 * 1024)
  SystemFileSystem.source(Path(path)).buffered().use { source ->
    bytes.usePinned { pinned ->
      while (true) {
        val count = source.readAtMostTo(bytes)
        if (count < 0) break
        CC_SHA256_Update(context.ptr, pinned.addressOf(0), count.toUInt())
      }
    }
  }
  val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
  digest.usePinned { CC_SHA256_Final(it.addressOf(0), context.ptr) }
  digest.toByteArray().toHexString()
}
