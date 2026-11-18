package me.zhangls.about

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import me.zhangls.about.api.NotesWebsite
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val ORIGIN = "https://website.example.test"
private const val MANIFEST = "/web-assets/manifest.json"
private const val SKIKO = "/0123456789abcdef0123.wasm"
private const val SKIKO_UPDATE = "/abcdef0123456789abcd.wasm"

class WebAssetCacheTest {
  @Test
  fun cachesOnlyMissingOrChangedAssetsAndChecksOncePerStartup() = runTest {
    val skiko = "skiko".encodeToByteArray()
    val font = "font".encodeToByteArray()
    val fontPath = "/web-assets/misans-${webAssetSha256(font)}.otf"
    val initial = WebAssetManifest(describe(SKIKO, skiko), describe(fontPath, font))
    val update = initial.copy(skiko = describe(SKIKO_UPDATE, skiko))
    val disk = MemoryWebAssetDisk()
    val transport = WebAssetTestEngine(initial, mapOf(SKIKO to skiko, SKIKO_UPDATE to skiko, fontPath to font))
    HttpClient(transport) { install(HttpTimeout) }.use { client ->
      val startup = WebAssetCache(NotesWebsite(ORIGIN), client) { disk }
      startup.refresh()
      startup.refresh()
      assertEquals(listOf(MANIFEST, SKIKO, fontPath), transport.requests)
      assertContentEquals(skiko, disk[SKIKO])
      assertContentEquals(font, disk[fontPath])

      WebAssetCache(NotesWebsite(ORIGIN), client) { disk }.refresh()
      assertEquals(listOf(MANIFEST, SKIKO, fontPath, MANIFEST), transport.requests)

      transport.manifest = update
      WebAssetCache(NotesWebsite(ORIGIN), client) { disk }.refresh()
      assertEquals(listOf(MANIFEST, SKIKO_UPDATE), transport.requests.takeLast(2))
      assertContentEquals(skiko, disk[SKIKO_UPDATE])
    }
  }

  @Test
  fun invalidOrIncompleteDownloadsKeepThePreviousCache() = runTest {
    val old = "old".encodeToByteArray()
    val expected = "new".encodeToByteArray()
    val fontPath = "/web-assets/misans-${webAssetSha256(expected)}.otf"
    val manifest = WebAssetManifest(describe(SKIKO_UPDATE, expected), describe(fontPath, expected))
    val disk = MemoryWebAssetDisk()
    disk.write(checkNotNull(cachedWebAsset(ORIGIN, ORIGIN + SKIKO)), old)
    val wrong = "bad".encodeToByteArray()
    val short = byteArrayOf(1)
    val transport = WebAssetTestEngine(manifest, mapOf(SKIKO_UPDATE to wrong, fontPath to short))
    HttpClient(transport) { install(HttpTimeout) }.use { client ->
      WebAssetCache(NotesWebsite(ORIGIN), client) { disk }.refresh()
    }
    assertEquals(listOf(MANIFEST, SKIKO_UPDATE, fontPath), transport.requests)
    assertContentEquals(old, disk[SKIKO])
    assertNull(disk[SKIKO_UPDATE])
    assertNull(disk[fontPath])
  }

  @Test
  fun cacheNamesRejectOtherOriginsAndResources() {
    assertEquals("application/wasm", cachedWebAsset(ORIGIN, ORIGIN + SKIKO)?.mimeType)
    assertNull(cachedWebAsset(ORIGIN, "https://foreign.example.test" + SKIKO))
    assertNull(cachedWebAsset(ORIGIN, "http://website.example.test" + SKIKO))
    assertNull(cachedWebAsset(ORIGIN, "$ORIGIN$SKIKO?version=1"))
    assertNull(cachedWebAsset(ORIGIN, "$ORIGIN/notes.js"))
  }

  @Test
  fun usesTheManifestSha256Format() {
    assertEquals(
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
      webAssetSha256("abc".encodeToByteArray()),
    )
  }

  private fun describe(path: String, bytes: ByteArray) = WebAsset(path, bytes.size, webAssetSha256(bytes))
}

private class MemoryWebAssetDisk : WebAssetDisk {
  private val files = mutableMapOf<CachedWebAsset, ByteArray>()

  operator fun get(path: String): ByteArray? = cachedWebAsset(ORIGIN, ORIGIN + path)?.let(::read)
  override fun read(asset: CachedWebAsset): ByteArray? = files[asset]
  override fun write(asset: CachedWebAsset, bytes: ByteArray) {
    files[asset] = bytes
  }
}

private class WebAssetTestEngine(
  var manifest: WebAssetManifest,
  private val assets: Map<String, ByteArray>,
) : HttpClientEngineBase("WebAssetTest") {
  override val config = HttpClientEngineConfig()
  override val supportedCapabilities = setOf(HttpTimeoutCapability)
  val requests = mutableListOf<String>()

  @OptIn(InternalAPI::class)
  override suspend fun execute(data: HttpRequestData): HttpResponseData {
    val path = data.url.encodedPath
    requests += path
    val bytes = if (path == MANIFEST) Json.encodeToString(manifest).encodeToByteArray() else assets.getValue(path)
    return HttpResponseData(
      HttpStatusCode.OK,
      GMTDate(),
      Headers.Empty,
      HttpProtocolVersion.HTTP_1_1,
      ByteReadChannel(bytes),
      callContext(),
    )
  }
}
