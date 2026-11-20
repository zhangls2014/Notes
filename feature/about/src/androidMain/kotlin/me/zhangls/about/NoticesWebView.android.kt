package me.zhangls.about

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
  resourceOwner: NoticesWebSession?,
) {
  val scope = rememberCoroutineScope()
  val geometrySource = rememberDeviceViewportGeometrySource(enabled = !projectPage)
  val retained = resourceOwner?.resource as? AndroidNoticesWebResource
  val publisher = remember(state) { retained?.publisher ?: DeviceViewportPublisher(url) }
  var bridgeView by remember { mutableStateOf<WebView?>(null) }
  BindAndroidDeviceViewport(bridgeView, projectPage, state, publisher, geometrySource)

  val surface = MaterialTheme.colorScheme.surface
  val backgroundColor = surface.toArgb()
  val openDetail = rememberUpdatedState(onOpenDetail)
  val inset = rememberUpdatedState(bottomInset.value)
  AndroidView(
    modifier = modifier.background(surface).deviceViewportGeometry(geometrySource),
    factory = { context ->
      val webView = retained?.view ?: createNoticesWebView(context, resourceOwner != null)
      if (webView == null) {
        state.fail()
        return@AndroidView View(context)
      }
      bridgeView = webView
      webView.apply {
        val disk = AndroidWebAssetDisk(context)
        // Keep the themed container visible until Chromium can draw the new document.
        visibility = if (state.loading) View.INVISIBLE else View.VISIBLE
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
            publisher.unbind()
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
            if (!projectPage && publisher.trusted(state.currentUrl)) {
              view.bindDeviceViewport(publisher, geometrySource) { generation == pageGeneration && !state.failed }
            }
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
        if (retained == null) {
          resourceOwner?.resource = AndroidNoticesWebResource(this, publisher)
          if (initialLink == NoticesLink.InPage) loadUrl(startUrl) else state.fail()
        }
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
      if (it is WebView) {
        if (resourceOwner == null) it.destroy() else it.webViewClient = WebViewClient()
      }
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


private class AndroidNoticesWebResource(
  val view: WebView,
  val publisher: DeviceViewportPublisher,
) : NoticesWebResource {
  override fun release() {
    view.stopLoading()
    view.destroy()
  }
}

@Composable
private fun BindAndroidDeviceViewport(
  bridgeView: WebView?,
  projectPage: Boolean,
  state: NoticesWebState,
  publisher: DeviceViewportPublisher,
  geometrySource: DeviceViewportGeometrySource?,
) {
  DisposableEffect(bridgeView, projectPage) {
    val view = bridgeView
    val observer = view?.viewTreeObserver
    val listener = ViewTreeObserver.OnPreDrawListener {
      val ready = !state.loading && !state.failed
      if (view != null && ready && geometrySource != null) {
        val geometry = geometrySource.snapshot()
        publisher.publish(state.currentUrl, geometry.features, geometry.availability, geometry.key)?.let {
          view.evaluateJavascript(it, null)
        }
      }
      true
    }
    if (!projectPage) observer?.addOnPreDrawListener(listener)
    geometrySource?.onFactsChanged = { view?.postInvalidateOnAnimation() }
    onDispose {
      geometrySource?.onFactsChanged = null
      if (observer?.isAlive == true) observer.removeOnPreDrawListener(listener)
      if (!projectPage) publisher.detachScript()?.let { view?.evaluateJavascript(it, null) }
      publisher.reset()
    }
  }
}

private fun WebView.bindDeviceViewport(
  publisher: DeviceViewportPublisher,
  source: DeviceViewportGeometrySource?,
  isCurrent: () -> Boolean,
) {
  evaluateJavascript(publisher.documentScript()) { response ->
    if (isCurrent()) {
      viewportDocumentId(response)?.let { id ->
        publisher.bindDocument(id)
        val geometry = source?.snapshot() ?: DeviceViewportGeometry()
        publisher.publish(url.orEmpty(), geometry.features, geometry.availability, geometry.key)?.let {
          evaluateJavascript(it, null)
        }
      }
    }
  }
}

private fun createNoticesWebView(context: Context, retained: Boolean): WebView? = try {
  WebView(if (retained) context.applicationContext else context)
} catch (_: RuntimeException) {
  null
}
