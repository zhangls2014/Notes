package me.zhangls.profile

import com.mohamedrejeb.calf.io.KmpFile
import me.zhangls.data.util.AppFileManager
import org.koin.core.annotation.Factory

@Factory(binds = [AvatarStorage::class])
internal actual class AvatarSaver(private val fileManager: AppFileManager) : AvatarStorage {
  actual override suspend fun save(avatar: KmpFile): String? {
    val uri = avatar.uri
    val targetPath = fileManager.createImageFile(avatarFilename(fileManager.getFileExtension(uri)))
    return try {
      if (fileManager.copyUriToFile(uri, targetPath)) targetPath
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
