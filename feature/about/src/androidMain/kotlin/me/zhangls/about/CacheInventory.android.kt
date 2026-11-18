package me.zhangls.about

import android.content.Context
import org.koin.mp.KoinPlatform
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
