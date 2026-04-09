package me.zhangls.main.search.domain

import com.mohamedrejeb.calf.io.KmpFile
import me.zhangls.data.util.AppFileManager
import org.koin.core.annotation.Factory
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970


@Factory
actual class AvatarSaver(private val fileManager: AppFileManager) {
  actual suspend fun save(avatar: KmpFile): String? {
    val srcUrl = avatar.url
    val extension = fileManager.fileExtension(srcUrl).let { if (it.isNotBlank()) ".$it" else it }
    val filename = "avatar_${NSDate().timeIntervalSince1970.toLong()}$extension"
    val destPath = fileManager.getImageFilePath(filename)

    return if (fileManager.copyPathToFile(srcUrl, destPath)) destPath else null
  }

  actual suspend fun delete(path: String) {
    fileManager.deleteFile(path)
  }
}
