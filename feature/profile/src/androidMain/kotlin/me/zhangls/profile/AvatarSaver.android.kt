package me.zhangls.profile

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import com.mohamedrejeb.calf.io.KmpFile
import me.zhangls.data.util.AppFileManager
import org.koin.core.annotation.Factory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlin.math.roundToInt

@Factory(binds = [AvatarStorage::class])
internal actual class AvatarSaver(private val fileManager: AppFileManager) : AvatarStorage {
  actual override suspend fun save(avatar: KmpFile): String? {
    val uri = avatar.uri
    val targetPath = fileManager.createImageFile(avatarFilename(fileManager.getFileExtension(uri)))
    var copied = false
    return try {
      copied = fileManager.copyUriToFile(uri, targetPath)
      targetPath.takeIf { copied }
    } finally {
      if (!copied) fileManager.deleteFile(targetPath)
    }
  }

  actual override fun encodeForUpload(path: String): ByteArray {
    val image = ImageDecoder.decodeBitmap(ImageDecoder.createSource(File(path))) { decoder, info, _ ->
      val (width, height) = info.size.let { it.width to it.height }
      val scale = minOf(1.0, AVATAR_SIDE_PX.toDouble() / maxOf(width, height))
      decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
      val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
      val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
      decoder.setTargetSize(targetWidth, targetHeight)
    }
    return try {
      ByteArrayOutputStream().use { output ->
        check(image.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_QUALITY, output))
        if (output.size() > AVATAR_MAX_BYTES) {
          output.reset()
          check(image.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_FALLBACK_QUALITY, output))
        }
        output.toByteArray()
      }
    } finally {
      image.recycle()
    }
  }

  actual override fun storeJpeg(jpeg: ByteArray): String {
    val path = fileManager.createImageFile(avatarFilename("jpg"))
    return try {
      File(path).writeBytes(jpeg)
      path
    } catch (failure: IOException) {
      fileManager.deleteFile(path)
      throw failure
    }
  }

  actual override fun exists(path: String): Boolean = File(path).isFile

  actual override suspend fun delete(path: String) {
    if (isManagedAvatar(path, fileManager.getImageDir())) fileManager.deleteFile(path)
  }
}
