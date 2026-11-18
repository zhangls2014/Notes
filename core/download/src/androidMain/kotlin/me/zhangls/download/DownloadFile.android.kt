package me.zhangls.download

import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

internal actual fun truncateDownload(path: String, size: Long) {
  RandomAccessFile(path, "rw").use { it.setLength(size) }
}

internal actual fun downloadSha256(path: String): String {
  val digest = MessageDigest.getInstance("SHA-256")
  File(path).forEachBlock { bytes, count -> digest.update(bytes, 0, count) }
  return digest.digest().toHexString()
}
