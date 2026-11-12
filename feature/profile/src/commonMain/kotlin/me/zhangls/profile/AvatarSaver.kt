package me.zhangls.profile

import com.mohamedrejeb.calf.io.KmpFile
import org.koin.core.annotation.Factory
import kotlin.uuid.Uuid

@Factory(binds = [AvatarStorage::class])
internal expect class AvatarSaver : AvatarStorage {
  override suspend fun save(avatar: KmpFile): String?
  override fun encodeForUpload(path: String): ByteArray
  override fun storeJpeg(jpeg: ByteArray): String
  override fun exists(path: String): Boolean
  override suspend fun delete(path: String)
}

internal const val AVATAR_SIDE_PX = 512
internal const val AVATAR_MAX_BYTES = 256 * 1024
internal const val AVATAR_JPEG_QUALITY = 80
internal const val AVATAR_JPEG_FALLBACK_QUALITY = 60

/** 每次选择使用新路径，避免快速连续保存覆盖旧图片或复用 Coil 缓存。 */
internal fun avatarFilename(extension: String): String {
  val safeExtension = extension.takeIf { it.length in 1..10 && it.all(Char::isLetterOrDigit) }
  return "avatar_${Uuid.random()}" + safeExtension?.let { ".$it" }.orEmpty()
}

/** 只回收本功能在应用私有图片目录内创建的文件。 */
internal fun isManagedAvatar(path: String, directory: String): Boolean {
  val filename = path.removePrefix("$directory/")
  return path.startsWith("$directory/") && filename.startsWith("avatar_") && '/' !in filename
}
