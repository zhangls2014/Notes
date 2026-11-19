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
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import me.zhangls.about.api.OnlineNoticeDetailDestination
import kotlin.coroutines.resume

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
  allowSameOriginNavigation: Boolean,
) {
  val scope = rememberCoroutineScope()
  val surface = MaterialTheme.colorScheme.surface
  val backgroundColor = surface.toArgb()
  val openDetail = rememberUpdatedState(onOpenDetail)
  val inset = rememberUpdatedState(bottomInset.value)
  AndroidView(
    modifier = modifier.background(surface),
    factory = { context ->
      val webView = try {
        WebView(context)
      } catch (_: RuntimeException) {
        state.fail()
        return@AndroidView View(context)
      }
      webView.apply {
        val disk = AndroidWebAssetDisk(context)
        // Keep the themed container visible until Chromium can draw the new document.
        visibility = View.INVISIBLE
        setBackgroundColor(backgroundColor)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = allowSameOriginNavigation
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.setSupportMultipleWindows(true)
        var restoreScroll = initialScroll
        var pageGeneration = 0L
        var frameWait: Job? = null
        webViewClient = object : WebViewClient() {
          override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            if (projectPage || request.isForMainFrame || request.method != "GET") return null
            return cachedWebAsset(url, request.url.toString())?.let(disk::response)
          }

          override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val target = request.url.toString()
            val link = noticesLink(target, url, projectPage, allowSameOriginNavigation)
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
            frameWait?.cancel()
            frameWait = null
            view.visibility = View.INVISIBLE
            pageGeneration++
            if (noticesLink(target, url, projectPage, allowSameOriginNavigation) != NoticesLink.InPage) {
              view.stopLoading()
              state.fail()
            } else {
              state.currentUrl = target
              state.loading = true
            }
          }

          override fun onPageFinished(view: WebView, url: String) {
            if (state.failed || frameWait != null) return
            if (!projectPage) view.applyNoticesInset(inset.value)
            val generation = pageGeneration
            frameWait = scope.launch {
              val ready = projectPage || awaitWebPageFrame {
                suspendCancellableCoroutine { continuation ->
                  view.evaluateJavascript(WEB_PAGE_FRAME_SCRIPT) {
                    if (continuation.isActive) continuation.resume(it == "\"ready\"")
                  }
                }
              }
              if (generation != pageGeneration || state.failed) return@launch
              if (ready) revealWhenDrawable(view) else state.fail()
            }
          }

          private fun revealWhenDrawable(view: WebView) {
            view.postVisualStateCallback(pageGeneration, object : WebView.VisualStateCallback() {
              override fun onComplete(requestId: Long) {
                if (requestId != pageGeneration || !view.isAttachedToWindow || state.failed) return
                if (restoreScroll > 0) {
                  view.scrollTo(0, restoreScroll.toInt())
                  restoreScroll = 0.0
                }
                view.visibility = View.VISIBLE
                state.loading = false
              }
            })
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
        state.reload = ::reload
        val startUrl = state.currentUrl
        val initialLink = noticesLink(startUrl, url, projectPage, allowSameOriginNavigation)
        if (initialLink == NoticesLink.InPage) loadUrl(startUrl) else state.fail()
      }
    },
    update = {
      it.setBackgroundColor(backgroundColor)
      if (it is WebView && !state.loading && !state.failed) {
        if (projectPage) it.setPadding(0, 0, (bottomInset.value * it.resources.displayMetrics.density).toInt(), 0)
        else it.applyNoticesInset(inset.value)
      }
    },
    onRelease = {
      scope.coroutineContext.cancelChildren()
      state.reload = null
      if (it is WebView) it.destroy()
    },
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
