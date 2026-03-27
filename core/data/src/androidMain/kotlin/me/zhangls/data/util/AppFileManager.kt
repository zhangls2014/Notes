package me.zhangls.data.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import org.koin.core.annotation.Singleton
import java.io.File

/**
 * 文件管理类，所有的文件获取都通过该类
 */
@Singleton
class AppFileManager(private val context: Context) {
  /**
   * 图片存储目录（私有目录）
   */
  fun getImageDir(): File {
    return File(getPrivateDir(), Environment.DIRECTORY_PICTURES)
      .apply {
        if (exists().not()) mkdirs()
      }
  }

  /**
   * 私有目录
   */
  private fun getPrivateDir(): File {
    val dir = context.filesDir
    return dir.apply {
      if (exists().not()) mkdirs()
    }
  }

  /**
   * 图片存储目录（私有目录）
   */
  fun getCacheImageDir(): File {
    return File(getCacheDir(), Environment.DIRECTORY_PICTURES)
      .apply {
        if (exists().not()) mkdirs()
      }
  }

  /**
   * 缓存目录
   */
  private fun getCacheDir(): File {
    val dir = context.cacheDir
    return dir.apply {
      if (exists().not()) mkdirs()
    }
  }

  /**
   * 根据文件名生成目标文件
   */
  fun createImageFile(fileName: String): File {
    return File(getImageDir(), fileName).apply {
      if (exists().not()) createNewFile()
    }
  }

  /**
   * 根据文件名生成目标文件
   */
  fun createCacheImageFile(fileName: String): File {
    return File(getCacheImageDir(), fileName).apply {
      if (exists().not()) createNewFile()
    }
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
  fun copyUriToFile(destUri: Uri, targetFile: File): Boolean {
    return try {
      context.contentResolver.openInputStream(destUri)?.use { input ->
        input.copyTo(targetFile.outputStream())
      }
      true
    } catch (e: Exception) {
      false
    }
  }
}