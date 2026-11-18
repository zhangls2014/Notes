package me.zhangls.about

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.zhangls.about.api.NotesWebsite
import me.zhangls.download.DownloadRequest
import me.zhangls.download.ResumableDownloader

private const val MANIFEST_BYTES = 8 * 1024
internal const val MAX_ASSET_BYTES = 32 * 1024 * 1024
private const val DOWNLOAD_TIMEOUT_MILLIS = 10 * 60 * 1000L
private val assetJson = Json { ignoreUnknownKeys = true }
private val skikoPath = Regex("/[a-f0-9]{20}\\.wasm")
private val miSansPath = Regex("/web-assets/misans-([a-f0-9]{20}(?:[a-f0-9]{44})?)\\.otf")
private val sha256Pattern = Regex("[a-f0-9]{64}")
private val cacheHost = Regex("[a-z0-9.-]+")

@Serializable
internal data class WebAssetManifest(val skiko: WebAsset, val miSans: WebAsset) {
  fun valid(): Boolean = skikoPath.matches(skiko.path) && skiko.valid() &&
    miSans.valid() && miSansPath.matchEntire(miSans.path)?.destructured?.let { (fingerprint) ->
      miSans.sha256.startsWith(fingerprint)
    } == true
}

@Serializable
internal data class WebAsset(val path: String, val size: Int, val sha256: String) {
  fun valid(): Boolean = size in 1..MAX_ASSET_BYTES && sha256Pattern.matches(sha256)
}

internal data class CachedWebAsset(val directory: String, val filename: String, val mimeType: String)

internal data class WebAssetTransfer(
  val file: CachedFile,
  val receivedBytes: Long,
  val active: Boolean = true,
)

internal fun cachedWebAsset(siteOrigin: String, resource: String): CachedWebAsset? {
  val origin = runCatching { Url(siteOrigin) }.getOrNull() ?: return null
  val host = origin.host.lowercase()
  return runCatching { Url(resource) }.getOrNull()
    ?.takeIf { origin.protocol == URLProtocol.HTTPS && cacheHost.matches(host) && it.cacheableBy(origin) }
    ?.asCachedWebAsset("$host-${origin.port}")
}

private fun Url.cacheableBy(origin: Url): Boolean {
  val sameOrigin = protocol == origin.protocol && host.equals(origin.host, ignoreCase = true) && port == origin.port
  val hasCredentials = user != null || password != null
  val hasSuffix = !parameters.isEmpty() || fragment.isNotEmpty()
  return sameOrigin && !hasCredentials && !hasSuffix
}

private fun Url.asCachedWebAsset(directory: String): CachedWebAsset? {
  val mimeType = when {
    skikoPath.matches(encodedPath) -> "application/wasm"
    miSansPath.matches(encodedPath) -> "font/otf"
    else -> return null
  }
  return CachedWebAsset(directory, encodedPath.substringAfterLast('/'), mimeType)
}

internal fun isWebAssetCacheFilename(name: String): Boolean =
  skikoPath.matches("/$name") || miSansPath.matches("/web-assets/$name")

internal expect fun webAssetDownloadPath(asset: CachedWebAsset): String?

internal fun interface WebAssetDownloader {
  suspend fun download(request: DownloadRequest, onProgress: (Long) -> Unit)
  suspend fun prepare(requests: List<DownloadRequest>) = Unit
  suspend fun hasActiveDownload(relativePath: String): Boolean = false
}

internal expect fun platformWebAssetDownloader(client: HttpClient): WebAssetDownloader
internal expect suspend fun scheduleWebAssetRefresh(cache: WebAssetCache)

internal fun webAssetHttpDownloader(client: HttpClient): WebAssetDownloader {
  val downloader = ResumableDownloader(client)
  return WebAssetDownloader { request, progress ->
    downloader.download(request.url, request.destination, request.size, request.sha256, DOWNLOAD_TIMEOUT_MILLIS, progress)
  }
}

internal class WebAssetCache(
  site: NotesWebsite,
  private val client: HttpClient,
  private val downloader: WebAssetDownloader = webAssetHttpDownloader(client),
  private val targetPath: (CachedWebAsset) -> String? = ::webAssetDownloadPath,
) {
  private val origin = site.origin.trimEnd('/')
  private val startupLock = Mutex()
  private val assetLock = Mutex()
  private val refreshingPaths = MutableStateFlow(emptySet<String>())
  private val clearedPaths = mutableSetOf<String>()
  private var checked = false
  private val activeTransfers = MutableStateFlow<Map<String, WebAssetTransfer>>(emptyMap())
  val transfers = activeTransfers.asStateFlow()
  private val publishedAssets = MutableStateFlow(0)
  val publicationCount = publishedAssets.asStateFlow()

  suspend fun refresh(): Boolean = withContext(Dispatchers.IO) {
    startupLock.withLock {
      if (checked) return@withLock true
      updateFromServer().also { checked = it }
    }
  }

  suspend fun clear(relativePath: String): Boolean = withContext(Dispatchers.IO) {
    assetLock.withLock {
      if (relativePath in refreshingPaths.value || downloader.hasActiveDownload(relativePath)) {
        return@withLock false
      }
      clearWebsiteFiles(relativePath).also { if (it) clearedPaths += relativePath }
    }
  }

  private suspend fun updateFromServer(): Boolean = coroutineScope {
    val manifest = readManifest() ?: return@coroutineScope false
    val downloads = assetLock.withLock {
      listOf(manifest.skiko, manifest.miSans).mapNotNull(::resourceDownload)
        .filterNot { (file, _) -> file.relativePath in clearedPaths }
        .also { assets -> refreshingPaths.value = assets.map { it.first.relativePath }.toSet() }
    }
    try {
      prepareDownloads(downloads.map { it.second })
      val results = downloads.map { (file, request) ->
        async {
          try {
            updateAsset(file, request)
            true
          } catch (cancelled: CancellationException) {
            throw cancelled
          } catch (_: Exception) {
            // Keep the previously validated copy when a download or write fails.
            false
          } finally {
            refreshingPaths.update { it - file.relativePath }
          }
        }
      }.awaitAll()
      results.all { it }
    } finally {
      refreshingPaths.value = emptySet()
    }
  }

  private suspend fun prepareDownloads(requests: List<DownloadRequest>) {
    try {
      downloader.prepare(requests)
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      // A standalone request can still pick up its own incomplete fragments.
    }
  }

  private suspend fun readManifest(): WebAssetManifest? =
    readManifest("manifest-v2.json") ?: readManifest("manifest.json")

  private suspend fun readManifest(name: String): WebAssetManifest? = try {
    downloadManifest(name)
      ?.let { assetJson.decodeFromString<WebAssetManifest>(it.decodeToString()) }
      ?.takeIf { it.valid() }
  } catch (cancelled: CancellationException) {
    throw cancelled
  } catch (_: Exception) {
    null
  }

  private fun resourceDownload(asset: WebAsset): Pair<CachedFile, DownloadRequest>? {
    val url = origin + asset.path
    val key = cachedWebAsset(origin, url) ?: return null
    val path = targetPath(key) ?: return null
    val source = Url(origin)
    val site = if (source.port == 443) source.host.lowercase() else "${source.host.lowercase()}:${source.port}"
    val file = CachedFile(
      key.filename,
      asset.size.toLong(),
      "$WEB_ASSET_CACHE_DIRECTORY/${key.directory}/${key.filename}",
      site,
    )
    return file to DownloadRequest(url, path, file.bytes, asset.sha256)
  }

  private suspend fun updateAsset(file: CachedFile, request: DownloadRequest) {
    try {
      downloader.download(request) { received ->
        activeTransfers.update { it + (request.destination to WebAssetTransfer(file, received)) }
      }
      publishedAssets.update { it + 1 }
    } finally {
      activeTransfers.update { it - request.destination }
    }
  }

  private suspend fun downloadManifest(name: String): ByteArray? =
    client.prepareGet("$origin/web-assets/$name") {
      header(HttpHeaders.CacheControl, "no-cache")
    }.execute { response ->
      if (response.status != HttpStatusCode.OK) return@execute null
      response.bodyAsChannel().readBuffer(MANIFEST_BYTES.toLong() + 1).readByteArray()
        .takeIf { it.size <= MANIFEST_BYTES }
    }
}
