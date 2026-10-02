package me.zhangls.profile

import com.mohamedrejeb.calf.io.KmpFile
import me.zhangls.data.util.AppFileManager
import org.koin.core.annotation.Factory

@Factory(binds = [AvatarStorage::class])
internal actual class AvatarSaver(private val fileManager: AppFileManager) : AvatarStorage {
  actual override suspend fun save(avatar: KmpFile): String? {
    val srcUrl = avatar.url
    val targetPath = fileManager.getImageFilePath(avatarFilename(fileManager.fileExtension(srcUrl)))
    return try {
      if (fileManager.copyPathToFile(srcUrl, targetPath)) targetPath
      else { fileManager.deleteFile(targetPath); null }
    } catch (error: Exception) {
      fileManager.deleteFile(targetPath)
      throw error
    }
  }

  actual override suspend fun delete(path: String) {
    if (isManagedAvatar(path, fileManager.getImageDir())) fileManager.deleteFile(path)
  }
}
