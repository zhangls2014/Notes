package me.zhangls.about

import android.graphics.Color
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import me.zhangls.about.api.OpenSourcePage
import notes.feature.about.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

private const val WEB_TEXT_ZOOM_PERCENT = 100

private class ScrollRestoration(var loading: Boolean = true, var pendingY: Int = 0)

@OptIn(ExperimentalResourceApi::class)
@Composable
internal actual fun NoticesWebView(
  page: OpenSourcePage,
  anchor: String?,
  appearance: NoticesAppearance,
  onNavigate: (OpenSourcePage, String?) -> Unit,
  onExternalLink: (String) -> Unit,
  onError: () -> Unit,
  modifier: Modifier,
  bottomInset: Dp,
) {
  val libraries = Res.getUri("files/open-source/android.html")
  val currentSelection = rememberUpdatedState(LocalNoticesLink(page, anchor))
  val navigate = rememberUpdatedState(onNavigate)
  val external = rememberUpdatedState(onExternalLink)
  val failed = rememberUpdatedState(onError)
  val inset = rememberUpdatedState(bottomInset.value)
  val savedY = rememberSaveable { mutableIntStateOf(0) }
  val restoration = remember { ScrollRestoration() }
  val uri = Uri.parse(noticesPageUri(libraries, page, anchor)).buildUpon()
    .apply { appearance.noticesAppearanceParameters().forEach { (name, value) -> appendQueryParameter(name, value) } }
    .build().toString()
  AndroidView(
    modifier = modifier,
    factory = { context ->
      WebView(context).apply {
        setBackgroundColor(Color.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.blockNetworkLoads = true
        settings.textZoom = WEB_TEXT_ZOOM_PERCENT
        setOnScrollChangeListener { _, _, y, _, _ ->
          if (!restoration.loading) savedY.intValue = y
        }
        webViewClient = object : WebViewClient() {
          override fun onPageFinished(view: WebView, url: String) {
            view.applyBottomContentInset(inset.value)
            if (restoration.loading && restoration.pendingY > 0) view.scrollTo(0, restoration.pendingY)
            restoration.loading = false
          }

          override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val target = request.url.toString()
            val local = localNoticesLink(target, libraries)
            if (local != null && local == currentSelection.value) return false
            if (local != null) {
              navigate.value(local.page, local.anchor)
            } else if (request.isForMainFrame && isExternalNoticesLink(target)) {
              external.value(target)
            }
            return true
          }

          override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) failed.value()
          }
        }
      }
    },
    update = { view ->
      view.applyBottomContentInset(inset.value)
      if (view.tag != uri) {
        restoration.pendingY = savedY.intValue
        restoration.loading = true
        view.tag = uri
        view.loadUrl(uri)
      }
    },
    onRelease = { view ->
      view.stopLoading()
      view.destroy()
    },
  )
}
