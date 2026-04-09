package me.zhangls.main.search.domain

import com.mohamedrejeb.calf.io.KmpFile
import me.zhangls.data.util.AppFileManager
import org.koin.core.annotation.Factory

/**
 * @author zhangls
 */
@Factory
actual class AvatarSaver(private val fileManager: AppFileManager) {
  actual suspend fun save(avatar: KmpFile): String? {
    val uri = avatar.uri
    val extension = fileManager.getFileExtension(uri).let { if (it.isNotBlank()) ".$it" else it }
    val filename = "avatar_${System.currentTimeMillis()}$extension"
    val targetPath = fileManager.createImageFile(filename)

    return if (fileManager.copyUriToFile(uri, targetPath)) targetPath else null
  }

  actual suspend fun delete(path: String) {
    fileManager.deleteFile(path)
  }
}
