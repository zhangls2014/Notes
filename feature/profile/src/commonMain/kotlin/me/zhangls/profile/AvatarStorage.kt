package me.zhangls.profile

import com.mohamedrejeb.calf.io.KmpFile

internal interface AvatarStorage {
  suspend fun save(avatar: KmpFile): String?
  fun encodeForUpload(path: String): ByteArray
  fun storeJpeg(jpeg: ByteArray): String
  fun exists(path: String): Boolean
  suspend fun delete(path: String)
}
