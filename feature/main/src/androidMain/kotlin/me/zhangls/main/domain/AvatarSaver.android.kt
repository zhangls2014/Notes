package me.zhangls.main.domain

import androidx.core.net.toUri
import me.zhangls.data.util.AppFileManager
import org.koin.core.annotation.Factory
import java.io.File

/**
 * @author zhangls
 */
@Factory
actual class AvatarSaver(private val fileManager: AppFileManager) {
  actual fun save(avatar: String): String? {
    val uri = avatar.toUri()
    val extension = fileManager.getFileExtension(uri)
    val filename = "avatar_${System.currentTimeMillis()}" + if (extension.isBlank()) "" else ".$extension"
    val targetFile = fileManager.createImageFile(filename)

    return if (fileManager.copyUriToFile(uri, targetFile)) {
      targetFile.path
    } else {
      null
    }
  }

  actual fun delete(path: String) {
    val file = File(path)
    if (file.exists()) {
      file.delete()
    }
  }
}