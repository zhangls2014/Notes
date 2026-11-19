package me.zhangls.entry

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import com.github.takahirom.roborazzi.captureRoboImage
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.CompletableDeferred
import me.zhangls.about.AboutEntryImpl
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.about.api.NotesWebsite
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.OnlineNoticeDetailDestination
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.koinInject
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.mp.KoinPlatform
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDownloadManager
import java.io.File

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w1000dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutUiTest(private val scenario: Scenario) {
  data class Scenario(val name: String, val width: Int, val height: Int, val fontScale: Float, val dark: Boolean) {
    override fun toString() = name
  }

  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    fun scenarios() = listOf(
      arrayOf(Scenario("compact", 320, 400, 1f, false)),
      arrayOf(Scenario("phone", 400, 800, 1f, false)),
      arrayOf(Scenario("large-font-dark", 320, 600, 2f, true)),
      arrayOf(Scenario("tablet", 900, 1000, 1f, false)),
    )
  }

  @get:Rule val compose = createComposeRule()

  @Before fun start() {
    initKoin { modules(module { single<Context> { RuntimeEnvironment.getApplication() } }) }
    provideAboutAppInfo()
  }

  @After fun stop() {
    stopKoin()
  }

  private lateinit var rootView: View
  private lateinit var websiteColors: ColorScheme

  private var darkTheme by mutableStateOf(scenario.dark)

  private fun launch(
    info: AboutAppInfo,
    channel: String? = null,
    restoration: StateRestorationTester? = null,
  ) {
    provideAboutAppInfo(info, channel)
    val content: @Composable () -> Unit = {
      rootView = LocalView.current.rootView
      Box(Modifier.requiredSize(scenario.width.dp, scenario.height.dp).testTag("about-page")) {
        ProvideWindowAdaptiveInfo(
          WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(scenario.width, scenario.height),
          Posture(),
        ) {
          ComposeAppTheme(darkTheme = darkTheme, fontScale = scenario.fontScale) {
            websiteColors = MaterialTheme.colorScheme
            var historyVisible by rememberSaveable { mutableStateOf(false) }
            var websiteVisible by rememberSaveable { mutableStateOf(false) }
            var cacheVisible by rememberSaveable { mutableStateOf(false) }
            val entry = koinInject<AboutEntry>()
            when {
              cacheVisible -> entry.CacheManagementScreen(onBack = { cacheVisible = false })
              historyVisible -> entry.ReleaseHistoryScreen(onBack = { historyVisible = false })
              websiteVisible -> entry.OfficialWebsiteScreen(onBack = { websiteVisible = false })
              else -> entry.Screen(
                onBack = {},
                onOpenReleaseHistory = { historyVisible = true },
                onOpenNotices = {},
                onOpenWebsite = { websiteVisible = true },
                onOpenCacheManagement = { cacheVisible = true },
              )
            }
          }
        }
      }
    }
    if (restoration == null) compose.setContent(content) else restoration.setContent(content)
  }

  @Test fun globalDisplaysPublishedNotesAndDisablesDownload() {
    verifyUpdateOffer("global")
  }

  @Test fun mainlandDisplaysPublishedNotesAndEnablesDownload() {
    verifyUpdateOffer("mainland")
  }

  @Test fun mainlandSwitchesDownloadOnlyAfterConfirmingNewerBuild() {
    verifyUpdateOffer("mainland", publishNewerBuild = true)
  }

  @Test fun mainlandDoesNotPromptToInstallSupersededBuild() {
    verifyUpdateOffer("mainland", publishNewerBuild = true, finishOlderBuild = true)
  }

  private fun verifyUpdateOffer(
    flavor: String,
    publishNewerBuild: Boolean = false,
    finishOlderBuild: Boolean = false,
  ) {
    val channel = "android-prd-$flavor"
    val site = NotesWebsite("https://website.example.test", "https://downloads.example.test")
    val updateUrl = if (flavor == "global") {
      "${site.origin}/releases/#v2.0.0"
    } else {
      "${site.origin}/downloads/android/$channel/2/"
    }
    val respond = CompletableDeferred<Unit>()
    val publishedBuild = java.util.concurrent.atomic.AtomicInteger(2)
    val engine = object : HttpClientEngineBase("AboutUpdateUiTest") {
      override val config = HttpClientEngineConfig()

      @OptIn(InternalAPI::class)
      override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val body = when (data.url.toString()) {
          "${site.origin}/updates/v1/$channel.json" -> {
            respond.await()
            val build = publishedBuild.get()
            val url = if (build == 2) updateUrl else "${site.origin}/downloads/android/$channel/$build/"
            """{"channel":"$channel","version":"$build.0.0","build":"$build","updateUrl":"$url"}"""
          }
          "${site.origin}/releases/published.json" ->
            """[{"version":"2.0.0","date":"2026-11-14","changes":[{"type":"added","platforms":["android"],"text":{"en":"Published update details","zh":"新版内容"}}]}]"""
          else -> error("Unexpected update request: ${data.url}")
        }
        return HttpResponseData(
          HttpStatusCode.OK, GMTDate(), Headers.Empty, HttpProtocolVersion.HTTP_1_1,
          ByteReadChannel(body), callContext(),
        )
      }
    }
    try {
      HttpClient(engine).use { client ->
        KoinPlatform.getKoin().declare(site)
        KoinPlatform.getKoin().declare(client, qualifier = named("publicUpdates"))
        launch(AboutAppInfo("1.0.0", "1"), flavor)
        compose.onNodeWithText("Check").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("Check").assertIsNotEnabled()
        compose.onNodeWithText("Notes 2.0.0 is available.").assertDoesNotExist()
        respond.complete(Unit)
        compose.waitUntil(timeoutMillis = 10_000) {
          compose.onAllNodes(hasText("Published update details", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Published update details", substring = true).performScrollTo().assertIsDisplayed()
        if (flavor == "global") {
          compose.onNodeWithText("Not supported").assertIsNotEnabled()
        } else {
          compose.onNodeWithText("Download in background").assertIsEnabled().performClick()
          val manager = RuntimeEnvironment.getApplication().getSystemService(DownloadManager::class.java)
          val downloads = Shadow.extract<ShadowDownloadManager>(manager)
          compose.waitUntil(timeoutMillis = 5_000) { downloads.requestCount == 1 }
          val request = Shadow.extract<ShadowDownloadManager.ShadowRequest>(downloads.getRequest(0L))
          assertEquals("${site.apkOrigin}/$channel/2/Notes-2.0.0-mainland.apk", request.uri.toString())
          compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("Notes 2.0.0 is available.")).fetchSemanticsNodes().isEmpty()
          }
          val downloading = compose.onNodeWithText("Downloading in the background…")
          downloading.assertDoesNotExist()
          compose.onNodeWithText("Check").assertIsEnabled().performClick()
          compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("Downloading in the background…")).fetchSemanticsNodes().isNotEmpty()
          }
          downloading.assertIsDisplayed()
          compose.onNodeWithText("Got it").performClick()
          downloading.assertDoesNotExist()
          compose.onNodeWithText("Check").assertIsEnabled().performClick()
          compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("Downloading in the background…")).fetchSemanticsNodes().isNotEmpty()
          }
          downloading.assertIsDisplayed()
          compose.onNodeWithText("Got it").performClick()
          assertEquals(1, downloads.requestCount)
          if (publishNewerBuild) {
            publishedBuild.set(3)
            compose.onNodeWithText("Check").performClick()
            compose.waitUntil(timeoutMillis = 5_000) {
              compose.onAllNodes(hasText("Notes 3.0.0 is available.")).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("An earlier version is still downloading.", substring = true).assertExists()
            assertEquals(1, downloads.requestCount)
            if (finishOlderBuild) {
              request.status = DownloadManager.STATUS_SUCCESSFUL
              compose.onNodeWithText("Got it").performClick()
              compose.onNodeWithText("Check").performClick()
              compose.waitUntil(timeoutMillis = 5_000) {
                compose.onAllNodes(hasText("Notes 3.0.0 is available.")).fetchSemanticsNodes().isNotEmpty()
              }
              compose.onNodeWithText("Notes 2.0.0 is ready to install.").assertDoesNotExist()
              return
            }
            compose.onNodeWithText("Download in background").performClick()
            compose.waitUntil(timeoutMillis = 5_000) { downloads.getRequest(1L) != null && downloads.requestCount == 1 }
            val replacement = Shadow.extract<ShadowDownloadManager.ShadowRequest>(downloads.getRequest(1L))
            assertEquals("${site.apkOrigin}/$channel/3/Notes-3.0.0-mainland.apk", replacement.uri.toString())
            replacement.status = DownloadManager.STATUS_SUCCESSFUL
            compose.waitUntil(timeoutMillis = 5_000) {
              compose.onAllNodes(hasText("Notes 3.0.0 is ready to install.")).fetchSemanticsNodes().isNotEmpty()
            }
            return
          }
          request.status = DownloadManager.STATUS_SUCCESSFUL
          compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodes(hasText("Install")).fetchSemanticsNodes().isNotEmpty()
          }
          compose.onNodeWithText("Notes 2.0.0 is ready to install.").assertIsDisplayed()
          compose.onNodeWithText("Install").assertIsEnabled()
        }
      }
    } finally {
      engine.close()
    }
  }

  @Test fun officialWebsiteLoadsInWebViewAndReturnsToAbout() {
    launch(AboutAppInfo("1.1.0", "2"))
    compose.onNodeWithText("Official website").performScrollTo().performClick()
    compose.onNodeWithText("About app").assertDoesNotExist()
    compose.runOnIdle {
      val webView = requireNotNull(findWebView(rootView))
      val url = Uri.parse(shadowOf(webView).lastLoadedUrl)
      assertEquals(Uri.parse(BuildKonfig.SITE_BASE_URL).authority, url.authority)
      assertEquals("/", url.path)
      assertEquals(null, url.getQueryParameter("embed"))
      assertEquals(if (scenario.dark) "dark" else "light", url.getQueryParameter("theme"))
      assertEquals(scenario.fontScale.toString(), url.getQueryParameter("fontScale"))
      mapOf(
        "accent" to websiteColors.primary,
        "on-primary" to websiteColors.onPrimary,
        "secondary-container" to websiteColors.secondaryContainer,
        "tertiary-container" to websiteColors.tertiaryContainer,
      ).forEach { (name, color) ->
        assertEquals(name, (color.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0'), url.getQueryParameter(name))
      }
    }
    compose.runOnIdle { darkTheme = !scenario.dark }
    compose.runOnIdle {
      val url = Uri.parse(shadowOf(requireNotNull(findWebView(rootView))).lastLoadedUrl)
      assertEquals(if (scenario.dark) "light" else "dark", url.getQueryParameter("theme"))
      assertEquals((websiteColors.primary.toArgb() and 0xFFFFFF).toString(16).padStart(6, '0'), url.getQueryParameter("accent"))
    }
    compose.onNodeWithContentDescription("Back").performClick()
    compose.onNodeWithText("About app").assertIsDisplayed()
  }

  @Test fun websiteRefreshReloadsTheCurrentPageAndRetriesItAfterFailure() {
    launch(AboutAppInfo("1.1.0", "2"))
    compose.onNodeWithText("Official website").performScrollTo().performClick()
    val refresh = compose.onNodeWithContentDescription("Refresh page").assertIsDisplayed()
    val original = compose.runOnIdle { requireNotNull(findWebView(rootView)) }
    val downloads = BuildKonfig.SITE_BASE_URL.trimEnd('/') + "/downloads/"
    compose.runOnIdle { shadowOf(original).pushEntryToHistory(downloads) }

    refresh.performClick()
    compose.runOnIdle {
      assertTrue(original === findWebView(rootView))
      assertEquals(downloads, original.url)
      assertEquals(1, shadowOf(original).reloadInvocations)
      original.webViewClient.onPageStarted(original, downloads, null)
      original.webViewClient.onRenderProcessGone(original, object : RenderProcessGoneDetail() {
        override fun didCrash() = true
        override fun rendererPriorityAtExit() = 0
      })
    }

    compose.onNodeWithText("Unable to load the official website.").assertIsDisplayed()
    refresh.performClick()
    compose.runOnIdle {
      val retried = requireNotNull(findWebView(rootView))
      assertEquals(downloads, shadowOf(retried).lastLoadedUrl)
    }
  }

  @Test fun cacheManagementListsStoredFilesAndCountsOnlyKnownCacheBytes() {
    val app = RuntimeEnvironment.getApplication()
    val cache = app.cacheDir
    fun store(path: String, bytes: Int) {
      File(cache, path).apply {
        parentFile?.mkdirs()
        writeBytes(ByteArray(bytes))
      }
    }

    val wasm = "0123456789abcdef0123.wasm"
    store("notes-web-assets/website.example.test-443/$wasm", 1_000)
    val font = "misans-${"a".repeat(64)}.otf"
    store("notes-web-assets/website.example.test-443/$font", 80)
    store("notes-web-assets/website.example.test-443/$wasm.new", 100)
    store("app-updates/Notes-42.apk", 300)
    store("other/private.txt", 80)
    val originalApkName = "Notes-2.0.0-mainland.apk"
    val downloadName = "download-$originalApkName"
    val directory = checkNotNull(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS))
    File(directory, downloadName).writeBytes(ByteArray(600))
    val manager = app.getSystemService(DownloadManager::class.java)
    val request = DownloadManager.Request(Uri.parse("https://downloads.example.test/$originalApkName"))
      .setDestinationInExternalFilesDir(app, Environment.DIRECTORY_DOWNLOADS, downloadName)
    val id = manager.enqueue(request)
    val shadow = Shadow.extract<ShadowDownloadManager>(manager)
    Shadow.extract<ShadowDownloadManager.ShadowRequest>(shadow.getRequest(id)).status = DownloadManager.STATUS_SUCCESSFUL
    val activeName = "active-$originalApkName"
    val activeFile = File(directory, activeName).apply { writeBytes(ByteArray(150)) }
    val activeId = manager.enqueue(
      DownloadManager.Request(Uri.parse("https://downloads.example.test/$activeName"))
        .setDestinationInExternalFilesDir(app, Environment.DIRECTORY_DOWNLOADS, activeName),
    )
    Shadow.extract<ShadowDownloadManager.ShadowRequest>(shadow.getRequest(activeId)).status = DownloadManager.STATUS_RUNNING

    launch(AboutAppInfo("1.1.0", "2"))
    for (heading in listOf("Version information", "Website and notices", "Storage")) {
      compose.onNodeWithText(heading).performScrollTo().assertIsDisplayed()
    }
    compose.onNodeWithText("Cache management").performScrollTo().performClick()
    compose.waitUntil(5_000) { compose.onAllNodes(hasText("2.0 KB")).fetchSemanticsNodes().isNotEmpty() }
    compose.onNodeWithText("Known cache size").assertIsDisplayed()
    compose.onNodeWithText("2.0 KB").assertIsDisplayed()
    compose.onNodeWithTag("about-page").captureRoboImage("build/about-candidates/cache-management-${scenario.name}.png")
    val list = compose.onNodeWithTag("cache-inventory")
    val wasmPath = "notes-web-assets/website.example.test-443/$wasm"
    list.performScrollToNode(hasText("Skiko WebAssembly"))
    val wasmName = compose.onNodeWithText("Skiko WebAssembly")
    wasmName.assertIsDisplayed()
    val titleBounds = wasmName.getUnclippedBoundsInRoot()
    val sizeBounds = compose.onNodeWithText("1.0 KB").getUnclippedBoundsInRoot()
    assertEquals(
      (titleBounds.top.value + titleBounds.bottom.value) / 2,
      (sizeBounds.top.value + sizeBounds.bottom.value) / 2,
      1f,
    )
    val wasmPathBounds = compose.onNodeWithText(wasmPath, substring = true).getUnclippedBoundsInRoot()
    assertTrue(wasmPathBounds.top >= titleBounds.bottom)
    assertTrue(wasmPathBounds.right >= sizeBounds.right)
    compose.onNodeWithText(wasmPath, substring = true).performScrollTo().assertIsDisplayed()
    val fontPath = "notes-web-assets/website.example.test-443/$font"
    list.performScrollToNode(hasText("MiSans font"))
    compose.onNodeWithText("MiSans font").assertIsDisplayed()
    compose.onNodeWithText(File(cache, fontPath).absolutePath).performScrollTo().assertIsDisplayed()
    list.performScrollToNode(hasText("Notes-42.apk"))
    val apkName = compose.onNodeWithText("Notes-42.apk").assertIsDisplayed()
    val apkPath = compose.onNodeWithText("app-updates/Notes-42.apk", substring = true)
    if (scenario.width > 320) {
      val name = apkName.getUnclippedBoundsInRoot()
      val path = apkPath.getUnclippedBoundsInRoot()
      val size = compose.onNodeWithText("300 B").getUnclippedBoundsInRoot()
      val nameCenter = (name.top.value + name.bottom.value) / 2
      val sizeCenter = (size.top.value + size.bottom.value) / 2
      assertEquals(nameCenter, sizeCenter, 1f)
      assertTrue(path.top >= name.bottom)
    }
    apkPath.performScrollTo().assertIsDisplayed()
    list.performScrollToNode(hasText(originalApkName))
    compose.onNodeWithText(originalApkName).assertIsDisplayed()
    compose.onNodeWithText(File(directory, downloadName).absolutePath).performScrollTo().assertIsDisplayed()
    list.performScrollToNode(hasText("Built-in browser cache"))
    compose.onNodeWithText("Built-in browser cache").assertIsDisplayed()

    val fontRowTag = "cache-row/$fontPath"
    list.performScrollToNode(hasTestTag(fontRowTag))
    val fontRow = compose.onNodeWithTag(fontRowTag)
    val fontIcon = compose.onNode(hasTestTag("cache-item-icon") and hasAnyAncestor(hasTestTag(fontRowTag)))
    val rowBounds = fontRow.getUnclippedBoundsInRoot()
    val iconBounds = fontIcon.getUnclippedBoundsInRoot()
    assertEquals((rowBounds.top.value + rowBounds.bottom.value) / 2, (iconBounds.top.value + iconBounds.bottom.value) / 2, 1f)
    val browserIcon = hasTestTag("cache-item-icon") and hasAnyAncestor(hasTestTag("cache-browser-note"))
    list.performScrollToNode(browserIcon)
    val browserNoteBounds = compose.onNodeWithTag("cache-browser-note").getUnclippedBoundsInRoot()
    val browserIconBounds = compose.onNode(browserIcon).getUnclippedBoundsInRoot()
    assertEquals(
      (browserNoteBounds.top.value + browserNoteBounds.bottom.value) / 2,
      (browserIconBounds.top.value + browserIconBounds.bottom.value) / 2,
      1f,
    )
    assertEquals(
      iconBounds.right.value - iconBounds.left.value,
      browserIconBounds.right.value - browserIconBounds.left.value,
      1f,
    )
    assertEquals(
      iconBounds.bottom.value - iconBounds.top.value,
      browserIconBounds.bottom.value - browserIconBounds.top.value,
      1f,
    )

    val deleteFont = compose.onNodeWithContentDescription("Delete $fontPath")
    list.performScrollToNode(hasTestTag(fontRowTag))
    deleteFont.assertDoesNotExist()
    fontRow.performTouchInput {
      down(center)
      moveTo(center.copy(x = center.x - 120.dp.toPx()))
    }
    deleteFont.assertIsDisplayed()
    assertTrue(File(cache, fontPath).exists())
    fontRow.performTouchInput { up() }
    deleteFont.assertIsDisplayed()
    assertTrue(File(cache, fontPath).exists())
    val wasmRowTag = "cache-row/$wasmPath"
    list.performScrollToNode(hasTestTag(wasmRowTag))
    compose.onNodeWithTag(wasmRowTag).performTouchInput { swipeLeft() }
    compose.onNodeWithContentDescription("Delete $wasmPath").assertIsDisplayed()
    list.performScrollToNode(hasTestTag(fontRowTag))
    deleteFont.assertDoesNotExist()
    fontRow.performTouchInput { swipeLeft() }
    list.performScrollToNode(hasTestTag(wasmRowTag))
    compose.onNodeWithContentDescription("Delete $wasmPath").assertDoesNotExist()
    list.performScrollToNode(hasTestTag(fontRowTag))
    deleteFont.assertIsDisplayed()
    fontRow.performTouchInput { swipeRight() }
    deleteFont.assertDoesNotExist()
    fontRow.performTouchInput { swipeLeft() }
    deleteFont.performTouchInput { click() }
    compose.onNode(hasText(fontPath, substring = true) and hasAnyAncestor(isDialog())).assertIsDisplayed()
    compose.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
    compose.waitUntil(5_000) { !File(cache, fontPath).exists() }
    assertTrue(File(cache, wasmPath).exists())
    assertTrue(File(cache, "$wasmPath.new").exists())
    assertTrue(File(cache, "other/private.txt").exists())
    assertTrue(shadow.getRequest(id) != null)

    val packageRow = "cache-row/Download/$downloadName"
    val clearPackage = "Delete Download/$downloadName"
    list.performScrollToNode(hasTestTag(packageRow))
    compose.onNodeWithTag(packageRow).performTouchInput { swipeLeft() }
    compose.waitUntil(5_000) {
      compose.onAllNodes(hasContentDescription(clearPackage) and isEnabled()).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithContentDescription(clearPackage).performClick()
    compose.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
    compose.waitUntil(5_000) { shadow.getRequest(id) == null }
    assertTrue(File(cache, "app-updates/Notes-42.apk").exists())
    list.performScrollToNode(hasText("1.3 KB"))
    compose.onNodeWithText("1.3 KB").assertIsDisplayed()
    assertTrue(shadow.getRequest(activeId) != null)
    assertTrue(activeFile.exists())
  }

  @Test fun cacheSwipeRestoresOpenAndClosedPositionsAcrossThemeAndPageRecreation() {
    val root = RuntimeEnvironment.getApplication().cacheDir
    val path = "app-updates/Notes-42.apk"
    File(root, path).apply {
      parentFile?.mkdirs()
      writeBytes(ByteArray(300))
    }
    val restoration = StateRestorationTester(compose)
    launch(AboutAppInfo("1.1.0", "2"), restoration = restoration)
    compose.onNodeWithText("Cache management").performScrollTo().performClick()
    val rowTag = "cache-row/$path"
    val list = compose.onNodeWithTag("cache-inventory")
    compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag(rowTag)).fetchSemanticsNodes().isNotEmpty() }
    list.performScrollToNode(hasTestTag(rowTag))
    val row = compose.onNodeWithTag(rowTag)
    val delete = compose.onNodeWithContentDescription("Delete $path")
    row.performTouchInput { swipeLeft() }
    delete.assertIsDisplayed()
    compose.runOnIdle { darkTheme = !darkTheme }
    delete.assertIsDisplayed()
    restoration.emulateSavedInstanceStateRestore()
    compose.waitUntil(5_000) {
      compose.onAllNodes(hasContentDescription("Delete $path")).fetchSemanticsNodes().isNotEmpty()
    }
    delete.assertIsDisplayed()
    row.performTouchInput { swipeRight() }
    delete.assertDoesNotExist()
    restoration.emulateSavedInstanceStateRestore()
    compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag(rowTag)).fetchSemanticsNodes().isNotEmpty() }
    delete.assertDoesNotExist()
    assertTrue(File(root, path).exists())
  }

  @Test fun cacheManagementShowsSavedProgressBeforeDownloadStarts() {
    val root = RuntimeEnvironment.getApplication().cacheDir
    val wasm = "0123456789abcdef0123.wasm"
    val checkpoint = File(root, "notes-web-assets/website.example.test-443/.$wasm.1048576.${"a".repeat(64)}.part")
    checkpoint.parentFile?.mkdirs()
    checkpoint.writeBytes(ByteArray(262_144))
    val otherSiteCheckpoint = File(root, "notes-web-assets/other.example.test-443/${checkpoint.name}")
    otherSiteCheckpoint.parentFile?.mkdirs()
    otherSiteCheckpoint.writeBytes(ByteArray(524_288))

    launch(AboutAppInfo("1.1.0", "2"))
    compose.onNodeWithText("Cache management").performScrollTo().performClick()
    compose.waitUntil(5_000) { compose.onAllNodes(hasText("0 B")).fetchSemanticsNodes().isNotEmpty() }
    val list = compose.onNodeWithTag("cache-inventory")
    list.performScrollToNode(hasText("Waiting to resume · 25%"))
    compose.onNodeWithText("Waiting to resume · 25%").assertIsDisplayed()
    compose.onNodeWithText("No cached files").assertDoesNotExist()
    val checkpointRow = "cache-row/notes-web-assets/website.example.test-443/$wasm"
    val clearCheckpoint = "Delete notes-web-assets/website.example.test-443/$wasm"
    list.performScrollToNode(hasTestTag(checkpointRow))
    compose.onNodeWithTag(checkpointRow).performTouchInput { swipeLeft() }
    compose.onNodeWithContentDescription(clearCheckpoint).performClick()
    compose.onNodeWithText("Cancel").performClick()
    assertTrue(checkpoint.exists())
    compose.onNodeWithTag(checkpointRow).performTouchInput { swipeLeft() }
    compose.onNodeWithContentDescription(clearCheckpoint).performClick()
    compose.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
    compose.waitUntil(5_000) { !checkpoint.exists() }
    compose.onNodeWithText("Waiting to resume · 25%").assertDoesNotExist()
    assertTrue(otherSiteCheckpoint.exists())
  }

  @Test fun websiteServesAnExactCachedAssetAndLeavesOtherRequestsOnline() {
    launch(AboutAppInfo("1.1.0", "2"))
    compose.onNodeWithText("Official website").performScrollTo().performClick()
    compose.runOnIdle {
      val webView = requireNotNull(findWebView(rootView))
      val origin = BuildKonfig.SITE_BASE_URL.trimEnd('/')
      val host = requireNotNull(Uri.parse(origin).host)
      val filename = "0123456789abcdef0123.wasm"
      val file = File(webView.context.cacheDir, "notes-web-assets/$host-443/$filename")
      file.parentFile?.mkdirs()
      file.writeText("cached Wasm")

      val hit = requireNotNull(webView.webViewClient.shouldInterceptRequest(webView, resourceRequest("$origin/$filename")))
      assertEquals("application/wasm", hit.mimeType)
      assertEquals("cached Wasm", hit.data.bufferedReader().use { it.readText() })
      for (url in listOf(
        "$origin/abcdef0123456789abcd.wasm",
        "$origin/notes.js",
        "https://foreign.example.test/$filename",
      )) {
        assertNull(webView.webViewClient.shouldInterceptRequest(webView, resourceRequest(url)))
      }
    }
  }

  private fun resourceRequest(url: String): WebResourceRequest = object : WebResourceRequest {
    override fun getUrl(): Uri = Uri.parse(url)
    override fun isForMainFrame() = false
    override fun isRedirect() = false
    override fun hasGesture() = false
    override fun getMethod() = "GET"
    override fun getRequestHeaders() = emptyMap<String, String>()
  }

  @Test fun releaseNotesLoadConfiguredWebsiteAndReturn() {
    launch(AboutAppInfo("1.1.0", "2"))
    compose.onNodeWithText("Release notes").assertDoesNotExist()
    compose.onNodeWithText("View release notes").assertDoesNotExist()
    compose.onNodeWithText("Version").performScrollTo().performClick()
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodes(hasText("2026-10-30")).fetchSemanticsNodes().isNotEmpty()
    }
    assertReleaseChanges()
    compose.onNodeWithText("View release notes").performScrollTo().performClick()
    compose.onNodeWithText("Release notes").assertIsDisplayed()
    compose.onNodeWithText("About app").assertDoesNotExist()
    compose.onNodeWithContentDescription("View in browser").assertDoesNotExist()
    compose.onNodeWithContentDescription("Refresh page").assertIsDisplayed().performClick()
    compose.runOnIdle {
      val webView = requireNotNull(findWebView(rootView))
      assertEquals(1, shadowOf(webView).reloadInvocations)
      val url = Uri.parse(shadowOf(webView).lastLoadedUrl)
      assertEquals(Uri.parse(BuildKonfig.SITE_BASE_URL).authority, url.authority)
      assertEquals("/releases/", url.path)
      assertEquals("android", url.getQueryParameter("platform"))
      assertEquals("1", url.getQueryParameter("embed"))
      assertEquals(scenario.fontScale.toString(), url.getQueryParameter("fontScale"))
      assertEquals(if (scenario.dark) "dark" else "light", url.getQueryParameter("theme"))
    }
    compose.onNodeWithContentDescription("Back").performClick()
    compose.onNodeWithText("Release notes").assertDoesNotExist()
    compose.onNodeWithText("Version").performScrollTo().performClick()
    compose.onNodeWithText("View release notes").assertDoesNotExist()
    compose.onNodeWithText("Update contents").assertDoesNotExist()
  }

  private fun findWebView(view: View): WebView? = when (view) {
    is WebView -> view
    is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findWebView(view.getChildAt(it)) }
    else -> null
  }

  @Test fun noticesReuseTheWebViewAfterLeavingAndReturningToComposition() {
    val about: AboutEntry = AboutEntryImpl(NotesWebsite("https://website.example.test"))
    val visible = mutableStateOf(true)
    compose.setContent {
      rootView = LocalView.current.rootView
      ComposeAppTheme {
        if (visible.value) {
          about.OnlineNoticesScreen(detail = null, onBack = {}, onOpenDetail = {})
        }
      }
    }
    val original = compose.runOnIdle { requireNotNull(findWebView(rootView)) }
    compose.runOnIdle { visible.value = false }
    compose.runOnIdle { assertEquals(null, findWebView(rootView)) }
    compose.runOnIdle { visible.value = true }
    compose.runOnIdle { assertTrue(original === findWebView(rootView)) }
  }

  @Test fun noticesListAndBothDetailTypesHaveWorkingRefreshActions() {
    val origin = "https://website.example.test"
    val about: AboutEntry = AboutEntryImpl(NotesWebsite(origin))
    val detail = mutableStateOf<OnlineNoticeDetailDestination?>(null)
    compose.setContent {
      rootView = LocalView.current.rootView
      ComposeAppTheme {
        about.OnlineNoticesScreen(detail = detail.value, onBack = {}, onOpenDetail = {})
      }
    }
    for (destination in listOf(
      null,
      OnlineNoticeDetailDestination(anchor = "license-MIT"),
      OnlineNoticeDetailDestination(projectUrl = "https://project.example.test/"),
    )) {
      compose.runOnIdle { detail.value = destination }
      compose.onNodeWithContentDescription("Refresh page").assertIsDisplayed().performClick()
      compose.runOnIdle {
        val webView = requireNotNull(findWebView(rootView))
        assertEquals(1, shadowOf(webView).reloadInvocations)
      }
    }
  }

  private fun assertReleaseChanges() {
    val expected = listOf(
      "Added",
      "• View release notes offline from About app.",
      "Improved",
      "• Release history adapts to wide windows with a multi-column waterfall layout.",
      "Fixed",
      "• Current-version notes, the startup dialog and release history now show the same categories and content.",
    )
    for (text in expected) {
      compose.onNodeWithText(text).performScrollTo().assertIsDisplayed()
    }
  }

  @Test fun missingCurrentVersionDoesNotShowAnotherVersionsNotes() {
    launch(AboutAppInfo("2.3.1", "42.7.1"))
    compose.onNodeWithText("Version").performScrollTo().performClick()
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodes(hasText("No release notes for this version.")).fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithText("No release notes for this version.").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("View release notes offline from About app.", substring = true).assertDoesNotExist()
    compose.onNodeWithText("View release notes").performScrollTo().assertIsDisplayed()
  }

  @Test fun noticesOpenAnInAppPage() {
    var opened = false
    val about: AboutEntry = AboutEntryImpl(NotesWebsite("https://website.example.test"))
    compose.setContent {
      ComposeAppTheme {
        about.Screen(onBack = {}, onOpenReleaseHistory = {}, onOpenNotices = { opened = true }, onOpenWebsite = {}, onOpenCacheManagement = {})
      }
    }
    compose.onNodeWithText("Open-source software notices").performScrollTo().assertIsEnabled().performClick()
    compose.runOnIdle { assertTrue(opened) }
  }

  @Test fun installedVersionExpandsUpdatesWithoutBuildNumber() {
    launch(AboutAppInfo("2.3.1", "42.7.1"))
    repeat(3) {
      compose.mainClock.advanceTimeBy(1000)
      compose.waitForIdle()
    }
    compose.onNodeWithTag("about-page").captureRoboImage("build/about-candidates/${scenario.name}.png")
    compose.onNodeWithText("2.3.1").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Build number").assertDoesNotExist()
    compose.onNodeWithText("Version").performClick()
    compose.onNodeWithText("Build number").assertDoesNotExist()
    compose.onNodeWithText("42.7.1").assertDoesNotExist()
    compose.onNodeWithText("Update contents").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Version").performScrollTo().performClick()
    compose.onNodeWithText("Build number").assertDoesNotExist()
    compose.onNodeWithText("Check for updates").performScrollTo().assertIsDisplayed()
    assertTrue(compose.onAllNodes(hasText("Check for updates") and hasClickAction()).fetchSemanticsNodes().isEmpty())
    compose.onNodeWithText("Not supported").performScrollTo().assertIsDisplayed().assertIsNotEnabled()
    val versionRowRight = compose.onNodeWithText("Version").getUnclippedBoundsInRoot().right.value
    val badgeTextRight = compose.onNodeWithText("Not supported").getUnclippedBoundsInRoot().right.value
    org.junit.Assert.assertEquals(
      "Update status aligns to the row's trailing inset",
      versionRowRight - 16f,
      badgeTextRight,
      1f,
    )
    val titleBounds = compose.onNodeWithText("Check for updates").getUnclippedBoundsInRoot()
    val detailBounds = compose.onNodeWithText("New versions and release notes").getUnclippedBoundsInRoot()
    val badgeBounds = compose.onNodeWithText("Not supported").getUnclippedBoundsInRoot()
    org.junit.Assert.assertEquals(
      "Update status is centered beside the title and description",
      (titleBounds.top.value + detailBounds.bottom.value) / 2f,
      (badgeBounds.top.value + badgeBounds.bottom.value) / 2f,
      1f,
    )
    repeat(3) {
      compose.mainClock.advanceTimeBy(1000)
      compose.waitForIdle()
    }
    compose.onNodeWithTag("about-page").captureRoboImage("build/about-candidates/${scenario.name}-updates.png")
    assertTrue("Update text and status do not overlap", titleBounds.right <= badgeBounds.left)
  }

  @Test fun missingVersionIsShownAsUnknown() {
    launch(AboutAppInfo(null, null))
    compose.onNodeWithText("Unknown").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("1.0").assertDoesNotExist()
  }

  @Test
  @Config(qualifiers = "zh-rCN-w1000dp-h1200dp-mdpi")
  fun chineseBrandAndNavigationLabelsAreLocalized() {
    launch(AboutAppInfo("2.3.1", "42.7.1"))
    compose.onNodeWithText("摘星").assertIsDisplayed()
    compose.onNodeWithText("关于应用").assertIsDisplayed()
    repeat(3) {
      compose.mainClock.advanceTimeBy(1000)
      compose.waitForIdle()
    }
    compose.onNodeWithTag("about-page").captureRoboImage("build/about-candidates/${scenario.name}-zh.png")
  }
}
