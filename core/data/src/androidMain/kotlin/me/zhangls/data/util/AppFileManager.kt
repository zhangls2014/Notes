package me.zhangls.data.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.datastore.dataStoreFile
import org.koin.core.annotation.Singleton
import java.io.File

/**
 * 文件管理类，所有的文件获取都通过该类
 */
@Singleton
actual class AppFileManager(private val context: Context) {

  actual fun getDatabasePath(filename: String): String {
    return context.getDatabasePath(filename).absolutePath
  }

  actual fun getDataStorePath(filename: String): String {
    return context.dataStoreFile(filename).absolutePath
  }

  /**
   * 图片存储目录（私有目录）
   */
  actual fun getImageDir(): String {
    return File(getPrivateDir(), Environment.DIRECTORY_PICTURES)
      .apply {
        if (exists().not()) mkdirs()
      }.absolutePath
  }

  /**
   * 私有目录
   */
  private fun getPrivateDir(): String {
    val dir = context.filesDir
    return dir.apply {
      if (exists().not()) mkdirs()
    }.absolutePath
  }

  /**
   * 图片存储目录（私有目录）
   */
  fun getCacheImageDir(): String {
    return File(getCacheDir(), Environment.DIRECTORY_PICTURES)
      .apply {
        if (exists().not()) mkdirs()
      }.absolutePath
  }

  /**
   * 缓存目录
   */
  private fun getCacheDir(): String {
    val dir = context.cacheDir
    return dir.apply {
      if (exists().not()) mkdirs()
    }.absolutePath
  }

  /**
   * 根据文件名生成目标文件
   */
  actual fun createImageFile(filename: String): String {
    return File(getImageDir(), filename).apply {
      if (exists().not()) createNewFile()
    }.absolutePath
  }

  /**
   * 根据文件名生成目标文件
   */
  fun createCacheImageFile(filename: String): String {
    return File(getCacheImageDir(), filename).apply {
      if (exists().not()) createNewFile()
    }.absolutePath
  }

  fun getFileName(uri: Uri): String? {
    var name: String? = null

    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
      val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (it.moveToFirst() && index >= 0) {
        name = it.getString(index)
      }
    }

    return name
  }

  fun getFileNameNoExtension(uri: Uri): String {
    val name = getFileName(uri)

    // 2. 如果没有扩展名，用 MIME 补
    return if (name.isNullOrBlank() || name.contains(".").not()) {
      context.contentResolver.getType(uri)?.substringBeforeLast("/") ?: ""
    } else {
      name.substringBeforeLast(".")
    }
  }

  fun getFileExtension(uri: Uri): String {
    val name = getFileName(uri)

    // 2. 如果没有扩展名，用 MIME 补
    return if (name.isNullOrBlank() || name.contains(".").not()) {
      context.contentResolver.getType(uri)?.substringAfterLast("/") ?: ""
    } else {
      name.substringAfterLast(".")
    }
  }

  /**
   * 将 Uri 对应的文件复制到目标文件
   */
  fun copyUriToFile(srcUri: Uri, destPath: String): Boolean {
    return try {
      context.contentResolver.openInputStream(srcUri)?.use { input ->
        input.copyTo(File(destPath).outputStream())
      }
      true
    } catch (_: Exception) {
      false
    }
  }

  actual fun deleteFile(path: String) {
    val file = File(path)
    if (file.exists()) {
      file.delete()
    }
  }
}