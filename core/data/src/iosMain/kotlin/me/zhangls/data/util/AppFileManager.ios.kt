package me.zhangls.data.util

import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.annotation.Singleton
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask


@OptIn(ExperimentalForeignApi::class)
@Singleton
actual class AppFileManager {
  actual fun getDatabasePath(filename: String): String {
    val databaseDir = getDatabaseDirectory()
    val filepath = "$databaseDir/$filename"
    return filepath
  }

  private fun getDatabaseDirectory(): String {
    val databaseDir = supportDir() + "/database"
    createDir(databaseDir)
    return databaseDir
  }

  actual fun getDataStorePath(filename: String): String {
    val datastoreDir = getDataStoreDirectory()
    val filepath = "$datastoreDir/$filename"
    return filepath
  }

  private fun getDataStoreDirectory(): String {
    val datastoreDir = supportDir() + "/datastore"
    createDir(datastoreDir)
    return datastoreDir
  }

  actual fun createImageFile(filename: String): String {
    val imageDir = getImageDir()
    val filepath = "$imageDir/$filename"
    createFile(filepath)
    return filepath
  }

  fun getImageFilePath(filename: String): String {
    val imageDir = getImageDir()
    val filepath = "$imageDir/$filename"
    return filepath
  }

  actual fun getImageDir(): String {
    val picturesDir = supportDir() + "/Pictures"
    createDir(picturesDir)
    return picturesDir
  }

  private fun supportDir(): String = getDir(NSApplicationSupportDirectory)

  private fun documentDir(): String = getDir(NSDocumentDirectory)

  private fun createDir(path: String) {
    val fileManager = NSFileManager.defaultManager
    if (fileManager.fileExistsAtPath(path).not()) {
      fileManager.createDirectoryAtPath(
        path = path,
        withIntermediateDirectories = true,
        attributes = null,
        error = null
      )
    }
  }

  private fun createFile(path: String): Boolean {
    val fileManager = NSFileManager.defaultManager
    return if (fileManager.fileExistsAtPath(path).not()) {
      fileManager.createFileAtPath(
        path = path,
        contents = null,
        attributes = null
      )
    } else {
      true
    }
  }

  private fun getDir(directory: ULong): String {
    val directory = NSFileManager.defaultManager.URLForDirectory(
      directory = directory,
      inDomain = NSUserDomainMask,
      appropriateForURL = null,
      create = true,
      error = null,
    )
    return requireNotNull(directory?.path)
  }

  fun fileExtension(filepath: NSURL): String {
    return filepath.pathExtension ?: ""
  }

  fun copyPathToFile(srcUrl: NSURL, destPath: String): Boolean {
    return try {
      val manager = NSFileManager.defaultManager
      val destUrl = NSURL.fileURLWithPath(destPath)
      val result = manager.copyItemAtURL(srcUrl, destUrl, null)
      result
    } catch (_: Exception) {
      false
    }
  }

  actual fun deleteFile(path: String) {
    val fileManager = NSFileManager.defaultManager
    if (fileManager.fileExistsAtPath(path)) {
      fileManager.removeItemAtPath(path, error = null)
    }
  }
}