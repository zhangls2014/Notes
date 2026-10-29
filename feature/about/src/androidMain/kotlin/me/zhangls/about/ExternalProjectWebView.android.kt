package me.zhangls.about

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView

/** Network pages never share the bundled notices view or its local resource access. */
@Composable
internal actual fun ExternalProjectWebView(
  url: String,
  onLoading: (Boolean) -> Unit,
  onError: () -> Unit,
  onTitle: (String?) -> Unit,
  modifier: Modifier,
  bottomInset: Dp,
) {
  val title = rememberUpdatedState(onTitle)
  val loading = rememberUpdatedState(onLoading)
  val failed = rememberUpdatedState(onError)
  val inset = rememberUpdatedState(bottomInset.value)
  AndroidView(
    modifier = modifier,
    factory = { context ->
      WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.blockNetworkLoads = false
        webChromeClient = object : WebChromeClient() {
          override fun onReceivedTitle(view: WebView, value: String?) {
            title.value(value)
          }
        }
        webViewClient = object : WebViewClient() {
          override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
            !isExternalNoticesLink(request.url.toString())

          override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            loading.value(true)
          }
          override fun onPageFinished(view: WebView, url: String) {
            view.applyBottomContentInset(inset.value)
            loading.value(false)
          }
          override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) failed.value()
          }
          override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
            if (request.isForMainFrame) failed.value()
          }
        }
      }
    },
    update = { view ->
      view.applyBottomContentInset(inset.value)
      if (view.tag != url) {
        view.tag = url
        if (isExternalNoticesLink(url)) view.loadUrl(url) else failed.value()
      }
    },
    onRelease = { view ->
      view.stopLoading()
      view.destroy()
    },
  )
}
