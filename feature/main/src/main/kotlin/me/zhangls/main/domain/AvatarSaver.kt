package me.zhangls.main.domain

import android.net.Uri
import me.zhangls.data.util.AppFileManager
import org.koin.core.annotation.Factory
import java.io.File

/**
 * @author zhangls
 */
@Factory
class AvatarSaver(private val fileManager: AppFileManager) {
  fun save(avatar: Uri): String? {
    val fileType = fileManager.getFileType(avatar)
    val filename = "avatar_${System.currentTimeMillis()}" + if (fileType.isBlank()) "" else ".$fileType"
    val targetFile = fileManager.createImageFile(filename)

    return if (fileManager.copyUriToFile(avatar, targetFile)) {
      targetFile.path
    } else {
      null
    }
  }

  fun delete(path: String) {
    val file = File(path)
    if (file.exists()) {
      file.delete()
    }
  }
}