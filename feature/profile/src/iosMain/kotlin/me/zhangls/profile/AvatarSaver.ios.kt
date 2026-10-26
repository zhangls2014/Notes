package me.zhangls.profile

import com.mohamedrejeb.calf.io.KmpFile
import me.zhangls.data.util.AppFileManager
import org.koin.core.annotation.Factory

@Factory(binds = [AvatarStorage::class])
internal actual class AvatarSaver(private val fileManager: AppFileManager) : AvatarStorage {
  actual override suspend fun save(avatar: KmpFile): String? {
    val srcUrl = avatar.url
    val targetPath = fileManager.getImageFilePath(avatarFilename(fileManager.fileExtension(srcUrl)))
    var copied = false
    return try {
      copied = fileManager.copyPathToFile(srcUrl, targetPath)
      targetPath.takeIf { copied }
    } finally {
      if (!copied) fileManager.deleteFile(targetPath)
    }
  }

  actual override suspend fun delete(path: String) {
    if (isManagedAvatar(path, fileManager.getImageDir())) fileManager.deleteFile(path)
  }
}
