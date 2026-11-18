package me.zhangls.about

import android.content.Context
import android.os.Environment
import org.koin.mp.KoinPlatform
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.NoSuchFileException
import java.nio.file.attribute.BasicFileAttributes

internal actual fun listCacheDirectory(relativePath: String): List<CacheDiskEntry> {
  val root = KoinPlatform.getKoin().get<Context>().cacheDir.toPath()
  val directory = root.resolve(relativePath)
  return try {
    if (!Files.readAttributes(directory, BasicFileAttributes::class.java, NOFOLLOW_LINKS).isDirectory) {
      return emptyList()
    }
    Files.newDirectoryStream(directory).use { entries ->
      entries.mapNotNull { path ->
        val status = try {
          Files.readAttributes(path, BasicFileAttributes::class.java, NOFOLLOW_LINKS)
        } catch (_: NoSuchFileException) {
          return@mapNotNull null
        }
        when {
          status.isDirectory -> CacheDiskEntry(path.fileName.toString(), bytes = null)
          status.isRegularFile -> CacheDiskEntry(path.fileName.toString(), status.size())
          else -> null
        }
      }
    }
  } catch (_: NoSuchFileException) {
    emptyList()
  }
}

internal actual suspend fun readWebEngineCacheSites(): List<String>? = null

internal actual fun listDownloadedUpdatePackages(): List<CachedFile> =
  KoinPlatform.getKoin().get<UpdateInstaller>().downloadedPackages()

internal actual fun deleteCacheFile(relativePath: String): Boolean {
  val path = KoinPlatform.getKoin().get<Context>().cacheDir.toPath().resolve(relativePath)
  return try {
    if (!Files.readAttributes(path, BasicFileAttributes::class.java, NOFOLLOW_LINKS).isRegularFile) return false
    Files.deleteIfExists(path)
    true
  } catch (_: NoSuchFileException) {
    true
  } catch (_: IOException) {
    false
  }
}

internal actual suspend fun clearUpdateCache(relativePath: String): Boolean =
  KoinPlatform.getKoin().get<UpdateInstaller>().clearCachedPackage(relativePath)

internal actual fun cacheFileAbsolutePath(relativePath: String): String {
  val app = KoinPlatform.getKoin().get<Context>()
  val prefix = "${Environment.DIRECTORY_DOWNLOADS}/"
  return if (relativePath.startsWith(prefix)) {
    val directory = checkNotNull(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))
    File(directory, relativePath.removePrefix(prefix)).absolutePath
  } else {
    File(app.cacheDir, relativePath).absolutePath
  }
}
