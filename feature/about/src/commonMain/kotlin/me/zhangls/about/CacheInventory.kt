package me.zhangls.about

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import me.zhangls.download.downloadCheckpoints

internal const val WEB_ASSET_CACHE_DIRECTORY = "notes-web-assets"
internal const val APP_UPDATE_CACHE_DIRECTORY = "app-updates"

internal data class CacheDiskEntry(val name: String, val bytes: Long?)
internal data class CachedFile(val name: String, val bytes: Long, val relativePath: String, val site: String? = null)

internal data class CacheInventory(
  val webFiles: List<CachedFile>,
  val webCheckpoints: List<WebAssetTransfer>,
  val updateFiles: List<CachedFile>,
  val webEngineSites: List<String>?,
) {
  val knownBytes: Long get() = webFiles.sumOf { it.bytes } + updateFiles.sumOf { it.bytes }
}

private val siteDirectory = Regex("([a-z0-9.-]+)-([0-9]+)")
private val updateApk = Regex("Notes-[0-9]+\\.apk")

internal suspend fun readCacheInventory(): CacheInventory {
  val (webFiles, webCheckpoints, updateFiles) = withContext(Dispatchers.IO) {
    val web = mutableListOf<CachedFile>()
    val checkpoints = mutableListOf<WebAssetTransfer>()
    for (entry in listCacheDirectory(WEB_ASSET_CACHE_DIRECTORY)) {
      val (host, port) = siteDirectory.matchEntire(entry.name)?.destructured ?: continue
      if (entry.bytes != null) continue
      val site = if (port == "443") host else "$host:$port"
      val directory = "$WEB_ASSET_CACHE_DIRECTORY/${entry.name}"
      val sizes = listCacheDirectory(directory)
        .mapNotNull { file -> file.bytes?.let { file.name to it } }.toMap()
      val completed = sizes.keys.filter(::isWebAssetCacheFilename)
      completed.mapTo(web) { name -> CachedFile(name, sizes.getValue(name), "$directory/$name", site) }
      downloadCheckpoints(sizes).filter {
        it.expectedBytes <= MAX_ASSET_BYTES && it.filename !in completed && isWebAssetCacheFilename(it.filename)
      }.groupBy { it.filename }.values.mapTo(checkpoints) { versions ->
        val checkpoint = versions.maxBy { it.savedBytes }
        WebAssetTransfer(
          CachedFile(checkpoint.filename, checkpoint.expectedBytes, "$directory/${checkpoint.filename}", site),
          checkpoint.savedBytes,
          active = false,
        )
      }
    }
    val updates = listCacheDirectory(APP_UPDATE_CACHE_DIRECTORY).mapNotNull { file ->
      file.bytes?.takeIf { updateApk.matches(file.name) }
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

internal expect fun listCacheDirectory(relativePath: String): List<CacheDiskEntry>
internal expect fun listDownloadedUpdatePackages(): List<CachedFile>
internal expect suspend fun readWebEngineCacheSites(): List<String>?
