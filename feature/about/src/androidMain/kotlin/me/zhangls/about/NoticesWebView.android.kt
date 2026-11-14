package me.zhangls.about

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import me.zhangls.about.api.OnlineNoticeDetailDestination
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal actual fun NoticesWebView(
  url: String,
  state: NoticesWebState,
  projectPage: Boolean,
  onOpenDetail: (OnlineNoticeDetailDestination, Double) -> Unit,
  initialScroll: Double,
  bottomInset: Dp,
  modifier: Modifier,
) {
  val backgroundColor = MaterialTheme.colorScheme.surface.toArgb()
  val openDetail = rememberUpdatedState(onOpenDetail)
  val inset = rememberUpdatedState(bottomInset.value)
  AndroidView(
    modifier = modifier,
    factory = { context ->
      val webView = try {
        WebView(context)
      } catch (_: RuntimeException) {
        state.fail()
        return@AndroidView View(context)
      }
      webView.apply {
        setBackgroundColor(backgroundColor)
        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.setSupportMultipleWindows(true)
        var restoreScroll = initialScroll
        webViewClient = object : WebViewClient() {
          override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val target = request.url.toString()
            val link = noticesLink(target, url, projectPage)
            if (link == NoticesLink.InPage) return false
            if (request.isForMainFrame) {
              when (link) {
                is NoticesLink.Detail -> openDetail.value(
                  OnlineNoticeDetailDestination(anchor = link.anchor), view.scrollY.toDouble(),
                )
                is NoticesLink.Project -> if (request.hasGesture() && !request.isRedirect) {
                  openDetail.value(OnlineNoticeDetailDestination(projectUrl = link.url), view.scrollY.toDouble())
                }
                else -> Unit
              }
            }
            return true
          }

          override fun onPageStarted(view: WebView, target: String, favicon: Bitmap?) {
            if (noticesLink(target, url, projectPage) != NoticesLink.InPage) {
              view.stopLoading()
              state.fail()
            } else {
              state.loading = true
            }
          }

          override fun onPageFinished(view: WebView, url: String) {
            if (!state.failed) {
              if (!projectPage) view.applyNoticesInset(inset.value)
              if (restoreScroll > 0) {
                view.postVisualStateCallback(0L, object : WebView.VisualStateCallback() {
                  override fun onComplete(requestId: Long) {
                    if (view.isAttachedToWindow) view.scrollTo(0, restoreScroll.toInt())
                    restoreScroll = 0.0
                  }
                })
              }
              state.loading = false
            }
          }

          override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) state.fail()
          }

          override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
            if (request.isForMainFrame) state.fail()
          }

          override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            state.fail()
            return true
          }
        }
        if (noticesLink(url, url, projectPage) == NoticesLink.InPage) loadUrl(url) else state.fail()
      }
    },
    update = {
      it.setBackgroundColor(backgroundColor)
      if (it is WebView && !state.loading && !state.failed) {
        if (projectPage) it.setPadding(0, 0, (bottomInset.value * it.resources.displayMetrics.density).toInt(), 0)
        else it.applyNoticesInset(inset.value)
      }
    },
    onRelease = { if (it is WebView) it.destroy() },
  )
}

private fun WebView.applyNoticesInset(bottom: Float) {
  evaluateJavascript(
    """
    (() => {
      if (!document.body) return;
      let spacer = document.getElementById('notes-safe-area-spacer');
      if (!spacer) {
        spacer = document.createElement('div');
        spacer.id = 'notes-safe-area-spacer';
        spacer.setAttribute('aria-hidden', 'true');
        document.body.appendChild(spacer);
      }
      spacer.style.height = '${bottom}px';
    })();
    """.trimIndent(),
    null,
  )
}
