package me.zhangls.about

import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.zhangls.about.api.NotesWebsite

private const val MANIFEST_BYTES = 8 * 1024
private const val MAX_ASSET_BYTES = 32 * 1024 * 1024
private const val DOWNLOAD_TIMEOUT_MILLIS = 10 * 60 * 1000L
private val assetJson = Json { ignoreUnknownKeys = true }
private val skikoPath = Regex("/[a-f0-9]{20}\\.wasm")
private val miSansPath = Regex("/web-assets/misans-([a-f0-9]{64})\\.otf")
private val sha256Pattern = Regex("[a-f0-9]{64}")
private val cacheHost = Regex("[a-z0-9.-]+")

@Serializable
internal data class WebAssetManifest(val skiko: WebAsset, val miSans: WebAsset) {
  fun valid(): Boolean = skikoPath.matches(skiko.path) && skiko.valid() &&
    miSansPath.matchEntire(miSans.path)?.groupValues?.get(1) == miSans.sha256 && miSans.valid()
}

@Serializable
internal data class WebAsset(val path: String, val size: Int, val sha256: String) {
  fun valid(): Boolean = size in 1..MAX_ASSET_BYTES && sha256Pattern.matches(sha256)
}

internal data class CachedWebAsset(val directory: String, val filename: String, val mimeType: String)

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

internal interface WebAssetDisk {
  fun read(asset: CachedWebAsset): ByteArray?
  fun write(asset: CachedWebAsset, bytes: ByteArray)
}

internal expect fun webAssetDisk(): WebAssetDisk?
internal expect fun webAssetSha256(bytes: ByteArray): String

internal class WebAssetCache(
  site: NotesWebsite,
  private val client: HttpClient,
  private val disk: () -> WebAssetDisk? = ::webAssetDisk,
) {
  private val origin = site.origin.trimEnd('/')
  private val startupLock = Mutex()
  private var checked = false

  suspend fun refresh() = withContext(Dispatchers.IO) {
    startupLock.withLock {
      if (!checked) {
        val storage = disk()
        if (storage != null) updateFromServer(storage)
        checked = true
      }
    }
  }

  private suspend fun updateFromServer(disk: WebAssetDisk) {
    val manifest = readManifest() ?: return
    for (asset in listOf(manifest.skiko, manifest.miSans)) {
      try {
        updateAsset(disk, asset)
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        // Keep the previously validated copy when a download or write fails.
      }
    }
  }

  private suspend fun readManifest(): WebAssetManifest? = try {
    download("$origin/web-assets/manifest.json", MANIFEST_BYTES)
      ?.let { assetJson.decodeFromString<WebAssetManifest>(it.decodeToString()) }
      ?.takeIf { it.valid() }
  } catch (cancelled: CancellationException) {
    throw cancelled
  } catch (_: Exception) {
    null
  }

  private suspend fun updateAsset(disk: WebAssetDisk, asset: WebAsset) {
    val url = origin + asset.path
    val key = cachedWebAsset(origin, url) ?: return
    val current = disk.read(key)
    if (current != null && asset.matches(current)) return

    download(url, asset.size, DOWNLOAD_TIMEOUT_MILLIS)
      ?.takeIf { asset.matches(it) }
      ?.let { disk.write(key, it) }
  }

  private suspend fun download(url: String, limit: Int, timeoutMillis: Long? = null): ByteArray? =
    client.prepareGet(url) {
      header(HttpHeaders.CacheControl, "no-cache")
      if (timeoutMillis != null) timeout { requestTimeoutMillis = timeoutMillis }
    }.execute { response ->
      if (response.status != HttpStatusCode.OK) return@execute null
      response.bodyAsChannel().readBuffer(limit.toLong() + 1).readByteArray().takeIf { it.size <= limit }
    }

  private fun WebAsset.matches(bytes: ByteArray): Boolean = bytes.size == size && webAssetSha256(bytes) == sha256
}
