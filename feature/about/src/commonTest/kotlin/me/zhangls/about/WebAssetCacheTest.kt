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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import me.zhangls.about.api.NotesWebsite
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val ORIGIN = "https://website.example.test"
private const val MANIFEST = "/web-assets/manifest-v2.json"
private const val LEGACY_MANIFEST = "/web-assets/manifest.json"
private const val SKIKO = "/0123456789abcdef0123.wasm"
private const val SKIKO_UPDATE = "/abcdef0123456789abcd.wasm"
private const val SKIKO_SHA = "0b041b443ea5a5f9b95a9ca5b68e5561315ff77cddd323fafb920586a540a379"
private const val FONT_SHA = "795ea3efa43d0872b63bf0067be97553b46983e4f075097669391e9d15388ecc"
private const val NEW_SHA = "11507a0e2f5e69d5dfa40a62a1bd7b6ee57e6bcd85c67c9b8431b36fff21c437"

class WebAssetCacheTest {
  @Test
  fun downloadsBothResourcesConcurrentlyAndPublishesEach() = runTest {
    withDisk { disk ->
      val bytes = "skiko".encodeToByteArray()
      val font = "font".encodeToByteArray()
      val fontPath = "/web-assets/misans-${FONT_SHA.take(20)}.otf"
      val skikoRequested = CompletableDeferred<Unit>()
      val fontRequested = CompletableDeferred<Unit>()
      val releaseResponse = CompletableDeferred<Unit>()
      val server = WebAssetTestEngine(
        WebAssetManifest(describe(SKIKO, bytes), describe(fontPath, font)),
        mapOf(SKIKO to bytes, fontPath to font),
      ) { path ->
        when (path) {
          SKIKO -> skikoRequested.complete(Unit)
          fontPath -> fontRequested.complete(Unit)
        }
        if (path != MANIFEST) releaseResponse.await()
      }
      HttpClient(server) { install(HttpTimeout) }.use { client ->
        val cache = WebAssetCache(NotesWebsite(ORIGIN), client, targetPath = disk::path)
        val refresh = launch { cache.refresh() }
        try {
          withContext(Dispatchers.Default) {
            withTimeout(5_000) {
              skikoRequested.await()
              fontRequested.await()
            }
          }
          assertEquals(
            WebAssetTransfer(
              CachedFile(
                SKIKO.removePrefix("/"),
                bytes.size.toLong(),
                "notes-web-assets/website.example.test-443$SKIKO",
                "website.example.test",
              ),
              0,
            ),
            cache.transfers.value.values.single { it.file.name.endsWith(".wasm") },
          )
          assertEquals(2, cache.transfers.value.size)
          assertNull(disk[SKIKO])
          assertNull(disk[fontPath])
        } finally {
          releaseResponse.complete(Unit)
        }
        refresh.join()
        assertEquals(emptyMap(), cache.transfers.value)
        assertEquals(2, cache.publicationCount.value)
        assertContentEquals(bytes, disk[SKIKO])
      }
    }
  }

  @Test
  fun cachesOnlyMissingOrChangedAssetsAndChecksOncePerStartup() = runTest {
    withDisk { disk ->
      val skiko = "skiko".encodeToByteArray()
      val font = "font".encodeToByteArray()
      val fontPath = "/web-assets/misans-${FONT_SHA.take(20)}.otf"
      val initial = WebAssetManifest(describe(SKIKO, skiko), describe(fontPath, font))
      val update = initial.copy(skiko = describe(SKIKO_UPDATE, skiko))
      val transport = WebAssetTestEngine(initial, mapOf(SKIKO to skiko, SKIKO_UPDATE to skiko, fontPath to font))
      HttpClient(transport) { install(HttpTimeout) }.use { client ->
        val startup = WebAssetCache(NotesWebsite(ORIGIN), client) { disk.path(it) }
        startup.refresh()
        startup.refresh()
        assertEquals(MANIFEST, transport.requests.first())
        assertEquals(setOf(SKIKO, fontPath), transport.requests.drop(1).toSet())
        assertContentEquals(skiko, disk[SKIKO])
        assertContentEquals(font, disk[fontPath])

        WebAssetCache(NotesWebsite(ORIGIN), client) { disk.path(it) }.refresh()
        assertEquals(MANIFEST, transport.requests.last())
        assertEquals(4, transport.requests.size)

        transport.manifest = update
        WebAssetCache(NotesWebsite(ORIGIN), client) { disk.path(it) }.refresh()
        assertEquals(listOf(MANIFEST, SKIKO_UPDATE), transport.requests.takeLast(2))
        assertContentEquals(skiko, disk[SKIKO_UPDATE])
      }
    }
  }

  @Test
  fun invalidOrIncompleteDownloadsKeepThePreviousCache() = runTest {
    withDisk { disk ->
      val old = "old".encodeToByteArray()
      val expected = "new".encodeToByteArray()
      val fontPath = "/web-assets/misans-${NEW_SHA.take(20)}.otf"
      val manifest = WebAssetManifest(describe(SKIKO_UPDATE, expected), describe(fontPath, expected))
      disk.write(checkNotNull(cachedWebAsset(ORIGIN, ORIGIN + SKIKO)), old)
      val wrong = "bad".encodeToByteArray()
      val short = byteArrayOf(1)
      val transport = WebAssetTestEngine(manifest, mapOf(SKIKO_UPDATE to wrong, fontPath to short))
      HttpClient(transport) { install(HttpTimeout) }.use { client ->
        WebAssetCache(NotesWebsite(ORIGIN), client) { disk.path(it) }.refresh()
      }
      assertEquals(mapOf(MANIFEST to 1, SKIKO_UPDATE to 1, fontPath to 3), transport.requests.groupingBy { it }.eachCount())
      assertContentEquals(old, disk[SKIKO])
      assertNull(disk[SKIKO_UPDATE])
      assertNull(disk[fontPath])
    }
  }

  @Test
  fun fallsBackToTheLegacyManifestWhenTheNewEndpointIsUnavailable() = runTest {
    withDisk { disk ->
      val skiko = "skiko".encodeToByteArray()
      val font = "font".encodeToByteArray()
      val legacyFont = "/web-assets/misans-${FONT_SHA}.otf"
      val manifest = WebAssetManifest(describe(SKIKO, skiko), describe(legacyFont, font))
      val server = WebAssetTestEngine(manifest, mapOf(SKIKO to skiko, legacyFont to font), legacy = true)
      HttpClient(server) { install(HttpTimeout) }.use { client ->
        assertTrue(WebAssetCache(NotesWebsite(ORIGIN), client) { disk.path(it) }.refresh())
      }
      assertEquals(listOf(MANIFEST, LEGACY_MANIFEST), server.requests.take(2))
      assertContentEquals(font, disk[legacyFont])
    }
  }

  @Test
  fun validatesTheShortFontFingerprintAgainstTheFullDigest() {
    val font = "font".encodeToByteArray()
    val shortPath = "/web-assets/misans-${FONT_SHA.take(20)}.otf"
    val manifest = WebAssetManifest(describe(SKIKO, "skiko".encodeToByteArray()), describe(shortPath, font))
    assertTrue(manifest.valid())
    assertFalse(manifest.copy(miSans = manifest.miSans.copy(sha256 = NEW_SHA)).valid())
  }

  @Test
  fun cacheNamesRejectOtherOriginsAndResources() {
    assertEquals("application/wasm", cachedWebAsset(ORIGIN, ORIGIN + SKIKO)?.mimeType)
    assertNull(cachedWebAsset(ORIGIN, "https://foreign.example.test" + SKIKO))
    assertNull(cachedWebAsset(ORIGIN, "http://website.example.test" + SKIKO))
    assertNull(cachedWebAsset(ORIGIN, "$ORIGIN$SKIKO?version=1"))
    assertNull(cachedWebAsset(ORIGIN, "$ORIGIN/notes.js"))
  }

  private fun describe(path: String, bytes: ByteArray) = WebAsset(
    path, bytes.size, when (bytes.decodeToString()) {
      "skiko" -> SKIKO_SHA
      "font" -> FONT_SHA
      "new" -> NEW_SHA
      else -> error("Unexpected test fixture")
    },
  )

  private suspend fun withDisk(action: suspend (TestWebAssetDisk) -> Unit) {
    val folder = Path(SystemTemporaryDirectory, "notes-web-cache-test-${Random.nextLong()}")
    SystemFileSystem.createDirectories(folder)
    try {
      action(TestWebAssetDisk(folder))
    } finally {
      SystemFileSystem.list(folder).forEach { SystemFileSystem.delete(it, mustExist = false) }
      SystemFileSystem.delete(folder)
    }
  }
}

private class TestWebAssetDisk(private val folder: Path) {
  fun path(asset: CachedWebAsset): String = Path(folder, asset.filename).toString()

  operator fun get(path: String): ByteArray? {
    val asset = checkNotNull(cachedWebAsset(ORIGIN, ORIGIN + path))
    val file = Path(path(asset))
    return if (SystemFileSystem.exists(file)) {
      SystemFileSystem.source(file).buffered().use { it.readByteArray() }
    } else {
      null
    }
  }

  fun write(asset: CachedWebAsset, bytes: ByteArray) {
    SystemFileSystem.sink(Path(path(asset))).buffered().use { it.write(bytes) }
  }
}

private class WebAssetTestEngine(
  var manifest: WebAssetManifest,
  private val assets: Map<String, ByteArray>,
  private val legacy: Boolean = false,
  private val beforeResponse: suspend (String) -> Unit = {},
) : HttpClientEngineBase("WebAssetTest") {
  override val config = HttpClientEngineConfig()
  override val supportedCapabilities = setOf(HttpTimeoutCapability)
  private val recorded = MutableStateFlow(emptyList<String>())
  val requests get() = recorded.value

  @OptIn(InternalAPI::class)
  override suspend fun execute(data: HttpRequestData): HttpResponseData {
    val path = data.url.encodedPath
    recorded.update { it + path }
    beforeResponse(path)
    val bytes = when (path) {
      if (legacy) LEGACY_MANIFEST else MANIFEST -> Json.encodeToString(manifest).encodeToByteArray()
      else -> assets[path]
    }
    return HttpResponseData(
      if (bytes == null) HttpStatusCode.NotFound else HttpStatusCode.OK,
      GMTDate(),
      Headers.Empty,
      HttpProtocolVersion.HTTP_1_1,
      ByteReadChannel(bytes ?: ByteArray(0)),
      callContext(),
    )
  }
}
