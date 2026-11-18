@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package me.zhangls.about

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSFileType
import platform.Foundation.NSFileTypeDirectory
import platform.Foundation.NSFileTypeRegular
import platform.Foundation.NSNumber
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.WebKit.WKWebsiteDataRecord
import platform.WebKit.WKWebsiteDataStore
import platform.WebKit.WKWebsiteDataTypeDiskCache
import kotlin.coroutines.resume

internal actual fun listDownloadedUpdatePackages(): List<CachedFile> = emptyList()

internal actual suspend fun clearUpdateCache(relativePath: String): Boolean = false

private fun cacheRoot(): String = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
  .filterIsInstance<String>().first()

internal actual fun deleteCacheFile(relativePath: String): Boolean {
  val manager = NSFileManager.defaultManager
  val path = "${cacheRoot()}/$relativePath"
  if (!manager.fileExistsAtPath(path)) return true
  return manager.attributesOfItemAtPath(path, null)?.get(NSFileType) == NSFileTypeRegular &&
    manager.removeItemAtPath(path, null)
}

internal actual fun listCacheDirectory(relativePath: String): List<CacheDiskEntry> {
  val manager = NSFileManager.defaultManager
  val directory = "${cacheRoot()}/$relativePath"
  if (!manager.fileExistsAtPath(directory)) return emptyList()
  val attributes = checkNotNull(manager.attributesOfItemAtPath(directory, null))
  if (attributes[NSFileType] != NSFileTypeDirectory) return emptyList()
  return checkNotNull(manager.contentsOfDirectoryAtPath(directory, null))
    .filterIsInstance<String>()
    .mapNotNull { name ->
      val entry = manager.attributesOfItemAtPath("$directory/$name", null) ?: return@mapNotNull null
      when (entry[NSFileType]) {
        NSFileTypeDirectory -> CacheDiskEntry(name, bytes = null)
        NSFileTypeRegular -> CacheDiskEntry(name, (entry[NSFileSize] as NSNumber).longLongValue)
        else -> null
      }
    }
}

internal actual suspend fun readWebEngineCacheSites(): List<String>? = withContext(Dispatchers.Main) {
  suspendCancellableCoroutine { continuation ->
    WKWebsiteDataStore.defaultDataStore().fetchDataRecordsOfTypes(setOf(WKWebsiteDataTypeDiskCache)) { records ->
      continuation.resume(records?.filterIsInstance<WKWebsiteDataRecord>()?.map { it.displayName }?.distinct()?.sorted())
    }
  }
}

internal actual fun cacheFileAbsolutePath(relativePath: String): String = "${cacheRoot()}/$relativePath"
