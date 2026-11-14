package me.zhangls.about

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import me.zhangls.about.api.NotesWebsite
import me.zhangls.framework.AppInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AvailableUpdateTest {
  private val site = NotesWebsite("https://website.example.test")

  @Test
  fun updateRequestsStayAnonymousAndClassifyFailures() = runTest {
    var responseStatus = HttpStatusCode.ServiceUnavailable
    var disconnect = false
    val appInfo = object : AppInfo {
      override fun getVersionCode() = 1L
      override fun getVersionName() = "1.0.0"
      override fun getBuildNumber() = "1"
    }
    val engine = object : HttpClientEngineBase("UpdatesTest") {
      override val config = HttpClientEngineConfig()

      @OptIn(InternalAPI::class)
      override suspend fun execute(data: HttpRequestData): HttpResponseData {
        assertNull(data.headers[HttpHeaders.Authorization])
        assertEquals("${site.origin}/updates/v1/android-prd-mainland.json", data.url.toString())
        if (disconnect) throw IOException("Test connection failure")
        return HttpResponseData(
          responseStatus,
          GMTDate(),
          Headers.Empty,
          HttpProtocolVersion.HTTP_1_1,
          ByteReadChannel("invalid manifest"),
          callContext(),
        )
      }
    }
    try {
      HttpClient(engine).use { client ->
        suspend fun failure() = assertFailsWith<UpdateCheckException> {
          checkAvailableUpdate(client, appInfo, "android-prd-mainland", site)
        }
        val http = failure()
        assertEquals(UpdateFailure.Http, http.reason)
        assertEquals(503, http.status)
        responseStatus = HttpStatusCode.OK
        assertEquals(UpdateFailure.Manifest, failure().reason)
        disconnect = true
        assertEquals(UpdateFailure.Transport, failure().reason)
      }
    } finally {
      engine.close()
    }
  }

  @Test
  fun mainlandUsesBuildNumberAndRequiresMatchingDownloadPath() {
    val url = "${site.origin}/downloads/android/android-prd-mainland/10/"
    val manifest = UpdateManifest(MAINLAND_UPDATE_CHANNEL, "1.10.0", "10", url)
    assertEquals(manifest, availableUpdate(manifest, manifest.channel, "9", site))
    assertNull(availableUpdate(manifest, manifest.channel, "10", site))
    assertFailsWith<IllegalArgumentException> {
      availableUpdate(manifest, GLOBAL_UPDATE_CHANNEL, "9", site)
    }
    assertFailsWith<IllegalArgumentException> {
      availableUpdate(manifest.copy(updateUrl = "https://example.com/"), manifest.channel, "9", site)
    }
  }

  @Test
  fun packagedRegionsAndLegacyMetadataIdentifyTheSameChannel() {
    assertEquals(MAINLAND_UPDATE_CHANNEL, publishedAndroidChannel("mainland"))
    assertEquals(MAINLAND_UPDATE_CHANNEL, publishedAndroidChannel("android-prd-mainland"))
    assertEquals(GLOBAL_UPDATE_CHANNEL, publishedAndroidChannel("global"))
    assertEquals(GLOBAL_UPDATE_CHANNEL, publishedAndroidChannel("android-prd-global"))
    assertNull(publishedAndroidChannel("android-prd-full"))
    assertNull(publishedAndroidChannel(null))
  }

  @Test
  fun globalUsesItsOwnChannelAndPublishedNotesUrl() {
    val manifest = UpdateManifest(GLOBAL_UPDATE_CHANNEL, "1.10.0", "10", "${site.origin}/releases/#v1.10.0")
    assertEquals(manifest, availableUpdate(manifest, GLOBAL_UPDATE_CHANNEL, "9", site))
    assertFailsWith<IllegalArgumentException> {
      availableUpdate(manifest.copy(channel = "android-prd-full"), GLOBAL_UPDATE_CHANNEL, "9", site)
    }
    assertFailsWith<IllegalArgumentException> {
      availableUpdate(manifest.copy(build = "../10"), GLOBAL_UPDATE_CHANNEL, "9", site)
    }
    assertTrue("ios-app-store" !in androidUpdateChannels)
  }
}
