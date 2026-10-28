package me.zhangls.entry

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import me.zhangls.about.aboutNavEntry
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.NoticesColors
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
    val info = mutableStateOf(AboutAppInfo("1.0", "1"))
    compose.setContent {
      rootView = LocalView.current.rootView
      val current = rememberUpdatedState(info.value)
      val entry = remember {
        entryProvider<NavKey> { aboutNavEntry(appInfo = { current.value }) {} }(OpenSourceNoticesDestination())
      }
      ComposeAppTheme { entry.Content() }
    }
    compose.runOnIdle {
      val view = requireNotNull(findWebView(rootView))
      val uri = Uri.parse(shadowOf(view).lastLoadedUrl)
      org.junit.Assert.assertEquals("file", uri.scheme)
      assertTrue(uri.path.orEmpty().endsWith("/android.html"))
      val path = requireNotNull(uri.path).removePrefix("/android_asset/")
      org.junit.Assert.assertEquals(
        setOf("android.html", "android-licenses.html"),
        view.context.assets.list(path.substringBeforeLast('/'))?.toSet(),
      )
      assertTrue(view.context.assets.open(path).bufferedReader().use { it.readText() }.contains("开源软件声明"))
      org.junit.Assert.assertFalse(view.settings.allowFileAccess)
      assertTrue(view.settings.blockNetworkLoads)
      info.value = info.value.copy(
        fontScale = 2f,
        darkTheme = true,
        pageColors = NoticesColors(
          background = "151713",
          card = "1e211b",
          foreground = "e4e7dd",
          muted = "b4baaa",
          outline = "373e31",
          accent = "aad395",
          primaryContainer = "354e16",
          onPrimaryContainer = "cdeda3",
        ),
      )
    }
    compose.runOnIdle {
      val uri = Uri.parse(shadowOf(requireNotNull(findWebView(rootView))).lastLoadedUrl)
      org.junit.Assert.assertEquals("2.0", uri.getQueryParameter("fontScale"))
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
            appInfo = AboutAppInfo("1.0", "1"),
            onBack = {},
            onNavigate = { page, anchor -> local += page to anchor },
          )
        }
      }
    }
    compose.runOnIdle {
      val view = requireNotNull(findWebView(rootView))
      val shadow = shadowOf(view)
      val details = shadow.lastLoadedUrl.substringBefore('?')
        .replace("android.html", "android-licenses.html") + "#license-MIT"
      assertTrue(shadow.webViewClient.shouldOverrideUrlLoading(view, webRequest(details)))
      org.junit.Assert.assertEquals(listOf(OpenSourcePage.LICENSES to "license-MIT"), local)
      assertTrue(shadow.webViewClient.shouldOverrideUrlLoading(view, webRequest("https://example.com/project")))
      org.junit.Assert.assertEquals(listOf("https://example.com/project"), external)
      assertTrue(shadow.webViewClient.shouldOverrideUrlLoading(view, webRequest("file:///etc/passwd")))
      org.junit.Assert.assertEquals(1, local.size)
      org.junit.Assert.assertEquals(1, external.size)
    }
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
              appInfo = AboutAppInfo("1.0", "1"),
              onBack = {},
              onNavigate = { _, _ -> },
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
