package me.zhangls.about

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import me.zhangls.download.downloadCheckpointFilename

private const val MANIFEST_FILENAME = ".manifest-v2.json"
private const val MANIFEST_LIMIT = 8 * 1024

internal enum class WebAssetKind { Skiko, MiSans }

internal fun WebAssetManifest.kind(filename: String): WebAssetKind? = when (filename) {
  skiko.path.substringAfterLast('/') -> WebAssetKind.Skiko
  miSans.path.substringAfterLast('/') -> WebAssetKind.MiSans
  else -> null
}

internal fun webAssetManifestDisk(
  directory: String,
  targetPath: (CachedWebAsset) -> String? = ::webAssetDownloadPath,
): WebAssetManifestDisk? = targetPath(CachedWebAsset(directory, MANIFEST_FILENAME, "application/json"))
  ?.let { WebAssetManifestDisk(Path(it)) }

internal class WebAssetManifestDisk(private val path: Path) {
  fun read(): WebAssetManifest? = try {
    val metadata = SystemFileSystem.metadataOrNull(path)
    if (metadata?.isRegularFile == true && metadata.size in 1..MANIFEST_LIMIT) {
      val bytes = SystemFileSystem.source(path).buffered().use { it.readByteArray() }
      Json.decodeFromString<WebAssetManifest>(bytes.decodeToString()).takeIf { it.valid() }
    } else {
      null
    }
  } catch (_: Exception) {
    null
  }

  fun write(manifest: WebAssetManifest) {
    require(manifest.valid())
    val directory = checkNotNull(path.parent)
    SystemFileSystem.createDirectories(directory)
    val temporary = Path(directory, "$MANIFEST_FILENAME.new")
    SystemFileSystem.sink(temporary).buffered().use { it.write(Json.encodeToString(manifest).encodeToByteArray()) }
    SystemFileSystem.atomicMove(temporary, path)
  }

  suspend fun prune(
    manifest: WebAssetManifest,
    checkpointsOnly: Boolean = false,
    isActive: suspend (String) -> Boolean,
  ): Boolean {
    val directory = checkNotNull(path.parent)
    if (SystemFileSystem.metadataOrNull(directory)?.isDirectory != true) return true
    var complete = true
    for (file in SystemFileSystem.list(directory)) {
      val checkpoint = downloadCheckpointFilename(file.name)
      val name = checkpoint ?: file.name
      val obsolete = isWebAssetCacheFilename(name) && manifest.kind(name) == null
      val selected = !checkpointsOnly || checkpoint != null
      if (obsolete && selected && isDirectFile(file, directory)) {
        if (isActive(name)) {
          complete = false
        } else {
          SystemFileSystem.delete(file, mustExist = false)
        }
      }
    }
    return complete
  }

  // Only direct, regular files belong to this cache. Do not follow symlinks.
  private fun isDirectFile(file: Path, directory: Path): Boolean =
    SystemFileSystem.metadataOrNull(file)?.isRegularFile == true &&
      SystemFileSystem.resolve(file) == Path(SystemFileSystem.resolve(directory), file.name)
}
