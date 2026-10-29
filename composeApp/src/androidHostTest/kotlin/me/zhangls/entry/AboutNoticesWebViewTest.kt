package me.zhangls.entry

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import me.zhangls.about.aboutNavEntry
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.OpenSourceNoticesDestination
import me.zhangls.about.api.OpenSourcePage
import me.zhangls.theme.ComposeAppTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.koinInject
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w1000dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutNoticesWebViewTest {
  @get:Rule val compose = createComposeRule()
  private lateinit var rootView: View

  @Before fun start() {
    initKoin(null)
  }

  @After fun stop() {
    stopKoin()
  }

  @Test fun bundledAndroidPageUsesLatestAppearanceInCachedEntry() {
    val changed = mutableStateOf(false)
    compose.setContent {
      rootView = LocalView.current.rootView
      val entry = remember {
        entryProvider<NavKey> {
          aboutNavEntry(
            paneMetadata = ::noticesPaneMetadata,
            embedProject = { false },
          ) {}
        }(OpenSourceNoticesDestination())
      }
      CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.5f),
      ) {
        ComposeAppTheme(
          fontScale = if (changed.value) 2f else 1f,
          darkTheme = changed.value,
          dynamicScheme = if (changed.value) {
            changedNoticesColors()
          } else {
            null
          },
        ) { entry.Content() }
      }
    }
    compose.runOnIdle {
      val view = requireNotNull(findWebView(rootView))
      val uri = Uri.parse(shadowOf(view).lastLoadedUrl)
      org.junit.Assert.assertEquals("file", uri.scheme)
      assertTrue(uri.path.orEmpty().endsWith("/android.html"))
      val path = requireNotNull(uri.path).removePrefix("/android_asset/")
      assertPackagedNotices(view, path)
      assertTrue(view.context.assets.open(path).bufferedReader().use { it.readText() }.contains("开源软件声明"))
      org.junit.Assert.assertFalse(view.settings.allowFileAccess)
      assertTrue(view.settings.blockNetworkLoads)
      changed.value = true
    }
    compose.runOnIdle {
      val uri = Uri.parse(shadowOf(requireNotNull(findWebView(rootView))).lastLoadedUrl)
      org.junit.Assert.assertEquals("3.0", uri.getQueryParameter("fontScale"))
      org.junit.Assert.assertEquals("dark", uri.getQueryParameter("theme"))
      org.junit.Assert.assertEquals("151713", uri.getQueryParameter("bg"))
      org.junit.Assert.assertEquals("1e211b", uri.getQueryParameter("card"))
      org.junit.Assert.assertEquals("e4e7dd", uri.getQueryParameter("fg"))
      org.junit.Assert.assertEquals("b4baaa", uri.getQueryParameter("muted"))
      org.junit.Assert.assertEquals("373e31", uri.getQueryParameter("line"))
      org.junit.Assert.assertEquals("aad395", uri.getQueryParameter("accent"))
      org.junit.Assert.assertEquals("354e16", uri.getQueryParameter("container"))
      org.junit.Assert.assertEquals("cdeda3", uri.getQueryParameter("on-container"))
    }
  }

  @Test fun localDetailsStayInTheAppAndProjectSourcesUseTheBrowser() {
    val external = mutableListOf<String>()
    val local = mutableListOf<Pair<OpenSourcePage, String?>>()
    val handler = object : UriHandler {
      override fun openUri(uri: String) {
        external += uri
      }
    }
    compose.setContent {
      rootView = LocalView.current.rootView
      ComposeAppTheme {
        CompositionLocalProvider(LocalUriHandler provides handler) {
          koinInject<AboutEntry>().NoticesScreen(
            page = OpenSourcePage.LIBRARIES,
            anchor = null,
            onBack = {},
            onNavigate = { page, anchor -> local += page to anchor },
            onExternalLink = { handler.openUri(it) },
          )
        }
      }
    }
    compose.runOnIdle {
      val view = requireNotNull(findWebView(rootView))
      val shadow = shadowOf(view)
      val details = shadow.lastLoadedUrl.substringBefore('?')
        .replace("android.html", "android-license-MIT.html")
      assertTrue(shadow.webViewClient.shouldOverrideUrlLoading(view, webRequest(details)))
      org.junit.Assert.assertEquals(listOf(OpenSourcePage.LICENSES to "license-MIT"), local)
      assertTrue(shadow.webViewClient.shouldOverrideUrlLoading(view, webRequest("https://example.com/project")))
      org.junit.Assert.assertEquals(listOf("https://example.com/project"), external)
      assertTrue(shadow.webViewClient.shouldOverrideUrlLoading(view, webRequest("file:///etc/passwd")))
      org.junit.Assert.assertEquals(1, local.size)
      org.junit.Assert.assertEquals(1, external.size)
    }
  }

  @Test fun selectedLicensesKeepTheLicenseTitle() {
    val selected = mutableStateOf("license-MIT")
    compose.setContent {
      rootView = LocalView.current.rootView
      ComposeAppTheme {
        koinInject<AboutEntry>().NoticesScreen(
          page = OpenSourcePage.LICENSES,
          anchor = selected.value,

          onBack = {},
          onNavigate = { _, _ -> },
          onExternalLink = {},
        )
      }
    }
    compose.onNodeWithText("License").assertExists()
    compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    compose.runOnIdle {
      val view = requireNotNull(findWebView(rootView))
      shadowOf(view).webViewClient.onPageFinished(view, shadowOf(view).lastLoadedUrl)
    }
    compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
    compose.runOnIdle { selected.value = "license-Apache-2.0" }
    compose.onNodeWithText("License").assertExists()
    compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
  }

  private fun assertPackagedNotices(view: WebView, path: String) {
    val packaged = view.context.assets.list(path.substringBeforeLast('/')).orEmpty().toSet()
    assertTrue("android.html" in packaged)
    assertTrue("android-license-MIT.html" in packaged)
    assertTrue(
      packaged.all {
        it == "android.html" || it.startsWith("android-license-") || it.startsWith("android-notice-")
      },
    )
    org.junit.Assert.assertFalse("android-licenses.html" in packaged)
  }

  @Test fun embeddedListRestoresReadingPositionAfterReturning() {
    val visible = mutableStateOf(true)
    compose.setContent {
      rootView = LocalView.current.rootView
      val holder = rememberSaveableStateHolder()
      ComposeAppTheme {
        if (visible.value) {
          holder.SaveableStateProvider("notices-list") {
            koinInject<AboutEntry>().NoticesScreen(
              page = OpenSourcePage.LIBRARIES,
              anchor = null,
              onBack = {},
              onNavigate = { _, _ -> },
              onExternalLink = {},
            )
          }
        }
      }
    }
    compose.runOnIdle {
      val view = requireNotNull(findWebView(rootView))
      shadowOf(view).webViewClient.onPageFinished(view, shadowOf(view).lastLoadedUrl)
      view.scrollTo(0, 250)
      visible.value = false
    }
    compose.runOnIdle { visible.value = true }
    compose.runOnIdle {
      val view = requireNotNull(findWebView(rootView))
      shadowOf(view).webViewClient.onPageFinished(view, shadowOf(view).lastLoadedUrl)
      org.junit.Assert.assertEquals(250, view.scrollY)
    }
  }

  @Test fun webpageViewportExtendsBehindGestureArea() {
    lateinit var composeView: View
    compose.setContent {
      composeView = LocalView.current
      rootView = composeView.rootView
      ComposeAppTheme {
        koinInject<AboutEntry>().NoticesScreen(
          page = OpenSourcePage.LIBRARIES,
          anchor = null,
          onBack = {},
          onNavigate = { _, _ -> },
          onExternalLink = {},
        )
      }
    }
    compose.runOnIdle {
      val insets = androidx.core.view.WindowInsetsCompat.Builder()
        .setInsets(
          androidx.core.view.WindowInsetsCompat.Type.navigationBars(),
          androidx.core.graphics.Insets.of(0, 0, 0, 40),
        )
        .setVisible(androidx.core.view.WindowInsetsCompat.Type.navigationBars(), true)
        .build()
      androidx.core.view.ViewCompat.dispatchApplyWindowInsets(composeView, insets)
    }
    val bounds = compose.onNodeWithTag("notices-webview").fetchSemanticsNode().boundsInRoot
    compose.runOnIdle {
      org.junit.Assert.assertEquals(composeView.height.toFloat(), bounds.bottom, 0.5f)
    }
  }

  private fun changedNoticesColors() = androidx.compose.material3.darkColorScheme(
    surface = androidx.compose.ui.graphics.Color(0xFF151713),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF1E211B),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE4E7DD),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB4BAAA),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF373E31),
    primary = androidx.compose.ui.graphics.Color(0xFFAAD395),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF354E16),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFCDEDA3),
  )

  private fun findWebView(view: View): WebView? = when (view) {
    is WebView -> view
    is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findWebView(view.getChildAt(it)) }
    else -> null
  }

  private fun webRequest(target: String) = object : WebResourceRequest {
    override fun getUrl(): Uri = Uri.parse(target)
    override fun isForMainFrame() = true
    override fun isRedirect() = false
    override fun hasGesture() = true
    override fun getMethod() = "GET"
    override fun getRequestHeaders() = emptyMap<String, String>()
  }
}
