package me.zhangls.about

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import me.zhangls.download.downloadCheckpoints
import me.zhangls.download.downloadCheckpointFilename

internal const val WEB_ASSET_CACHE_DIRECTORY = "notes-web-assets"
internal const val APP_UPDATE_CACHE_DIRECTORY = "app-updates"

internal data class CacheDiskEntry(val name: String, val bytes: Long?)
internal data class CachedFile(
  val name: String,
  val bytes: Long,
  val relativePath: String,
  val site: String? = null,
  val kind: WebAssetKind? = null,
)

internal data class CacheInventory(
  val webFiles: List<CachedFile>,
  val webCheckpoints: List<WebAssetTransfer>,
  val updateFiles: List<CachedFile>,
  val webEngineSites: List<String>?,
) {
  val knownBytes: Long get() = webFiles.sumOf { it.bytes } + updateFiles.sumOf { it.bytes }
}

private val siteDirectory = Regex("([a-z0-9.-]+)-([0-9]+)")
internal val cachedUpdateApk = Regex("Notes-[0-9]+\\.apk")

internal suspend fun readCacheInventory(): CacheInventory {
  val (webFiles, webCheckpoints, updateFiles) = withContext(Dispatchers.IO) {
    val web = mutableListOf<CachedFile>()
    val checkpoints = mutableListOf<WebAssetTransfer>()
    for (entry in listCacheDirectory(WEB_ASSET_CACHE_DIRECTORY)) {
      val (host, port) = siteDirectory.matchEntire(entry.name)?.destructured ?: continue
      if (entry.bytes != null) continue
      val site = if (port == "443") host else "$host:$port"
      val directory = "$WEB_ASSET_CACHE_DIRECTORY/${entry.name}"
      val manifest = webAssetManifestDisk(entry.name)?.read()
      val sizes = listCacheDirectory(directory)
        .mapNotNull { file -> file.bytes?.let { file.name to it } }.toMap()
      val completed = sizes.keys.filter(::isWebAssetCacheFilename)
      completed.mapTo(web) { name ->
        CachedFile(name, sizes.getValue(name), "$directory/$name", site, manifest?.kind(name))
      }
      downloadCheckpoints(sizes).filter {
        it.expectedBytes <= MAX_ASSET_BYTES && it.filename !in completed && isWebAssetCacheFilename(it.filename)
      }.groupBy { it.filename }.values.mapTo(checkpoints) { versions ->
        val checkpoint = versions.maxBy { it.savedBytes }
        WebAssetTransfer(
          CachedFile(
            checkpoint.filename,
            checkpoint.expectedBytes,
            "$directory/${checkpoint.filename}",
            site,
            manifest?.kind(checkpoint.filename),
          ),
          checkpoint.savedBytes,
          active = false,
        )
      }
    }
    val updates = listCacheDirectory(APP_UPDATE_CACHE_DIRECTORY).mapNotNull { file ->
      file.bytes?.takeIf { cachedUpdateApk.matches(file.name) }
        ?.let { CachedFile(file.name, it, "$APP_UPDATE_CACHE_DIRECTORY/${file.name}") }
    }
    Triple(
      web.sortedWith(compareBy(CachedFile::site, CachedFile::name)),
      checkpoints,
      (updates + listDownloadedUpdatePackages()).sortedBy(CachedFile::name),
    )
  }
  return CacheInventory(webFiles, webCheckpoints, updateFiles, readWebEngineCacheSites())
}

internal fun clearWebsiteFiles(relativePath: String): Boolean {
  val directory = relativePath.substringBeforeLast('/')
  val site = directory.removePrefix("$WEB_ASSET_CACHE_DIRECTORY/")
  val asset = relativePath.substringAfterLast('/')
  if (directory == site || !siteDirectory.matches(site) || !isWebAssetCacheFilename(asset)) return false
  val files = if (listCacheDirectory(WEB_ASSET_CACHE_DIRECTORY).any { it.name == site && it.bytes == null }) {
    listCacheDirectory(directory)
  } else {
    emptyList()
  }
  var cleared = true
  for (file in files) {
    val belongsToAsset = file.name == asset || downloadCheckpointFilename(file.name) == asset
    if (file.bytes != null && belongsToAsset && !deleteCacheFile("$directory/${file.name}")) {
      cleared = false
    }
  }
  return cleared
}

internal expect fun cacheFileAbsolutePath(relativePath: String): String

internal expect fun listCacheDirectory(relativePath: String): List<CacheDiskEntry>
internal expect fun listDownloadedUpdatePackages(): List<CachedFile>
internal expect fun deleteCacheFile(relativePath: String): Boolean
internal expect suspend fun clearUpdateCache(relativePath: String): Boolean
internal expect suspend fun readWebEngineCacheSites(): List<String>?
