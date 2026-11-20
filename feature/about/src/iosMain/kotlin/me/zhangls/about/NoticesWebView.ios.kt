package me.zhangls.about

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
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import me.zhangls.about.api.OnlineNoticeDetailDestination
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLRequest
import platform.QuartzCore.CADisplayLink
import platform.UIKit.UIColor
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIScrollViewContentInsetAdjustmentBehavior
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationResponse
import platform.WebKit.WKNavigationResponsePolicy
import platform.WebKit.WKNavigationTypeLinkActivated
import platform.WebKit.WKSnapshotConfiguration
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
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
  val surface = MaterialTheme.colorScheme.surface.convert(ColorSpaces.Srgb)
  val surfaceColor = remember(surface) {
    UIColor(
      red = surface.red.toDouble(),
      green = surface.green.toDouble(),
      blue = surface.blue.toDouble(),
      alpha = surface.alpha.toDouble(),
    )
  }
  val scope = rememberCoroutineScope()
  val geometrySource = rememberDeviceViewportGeometrySource(enabled = !projectPage)
  val retained = resourceOwner?.resource as? IosNoticesWebResource
  val publisher = remember(state) { retained?.publisher ?: DeviceViewportPublisher(url) }
  var bridgeView by remember { mutableStateOf<WKWebView?>(null) }
  BindIosDeviceViewport(bridgeView, projectPage, state, publisher, geometrySource)

  val openDetail = rememberUpdatedState(onOpenDetail)
  val delegate = remember(state) {
    object : NSObject(), WKNavigationDelegateProtocol {
      private var restoreScroll = initialScroll
      private var frameWait: Job? = null
      private var pageGeneration = 0L

      @ObjCSignatureOverride
      override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        cancelFrameWait()
        publisher.unbind()
        webView.hidden = true
        state.loading = true
      }

      @ObjCSignatureOverride
      override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        if (state.failed) return
        if (frameWait != null) return
        val generation = pageGeneration
        if (!projectPage && publisher.trusted(state.currentUrl)) {
          webView.bindDeviceViewport(publisher, geometrySource) { generation == pageGeneration && !state.failed }
        }
        frameWait = scope.launch {
          val ready = projectPage || awaitWebPageFrame {
            suspendCancellableCoroutine { continuation ->
              webView.evaluateJavaScript(WEB_PAGE_FRAME_SCRIPT) { value, _ ->
                if (continuation.isActive) continuation.resume(value == "ready")
              }
            }
          }
          if (generation != pageGeneration || state.failed) return@launch
          if (ready) finishLoading(webView, generation) else state.fail()
        }
      }

      fun cancelFrameWait() {
        pageGeneration++
        frameWait?.cancel()
        frameWait = null
      }

      private fun finishLoading(webView: WKWebView, generation: Long) {
        if (restoreScroll > 0) {
          webView.scrollView.setContentOffset(CGPointMake(0.0, restoreScroll), animated = false)
          restoreScroll = 0.0
        }
        val configuration = WKSnapshotConfiguration().apply { afterScreenUpdates = true }
        webView.takeSnapshotWithConfiguration(configuration) { image, error ->
          if (generation != pageGeneration || state.failed) return@takeSnapshotWithConfiguration
          if (image == null || error != null) {
            state.fail()
          } else {
            webView.hidden = false
            state.loading = false
          }
        }
      }

      @ObjCSignatureOverride
      override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
        if (withError.code != NSURLErrorCancelled) state.fail()
      }

      @ObjCSignatureOverride
      override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
        if (withError.code != NSURLErrorCancelled) state.fail()
      }

      override fun webViewWebContentProcessDidTerminate(webView: WKWebView) {
        state.fail()
      }

      override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationResponse: WKNavigationResponse,
        decisionHandler: (WKNavigationResponsePolicy) -> Unit,
      ) {
        val response = decidePolicyForNavigationResponse
        val status = (response.response as? NSHTTPURLResponse)?.statusCode
        if (response.isForMainFrame() && status != null && status >= 400) {
          decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyCancel)
          state.fail()
        } else {
          decisionHandler(WKNavigationResponsePolicy.WKNavigationResponsePolicyAllow)
        }
      }

      override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
      ) {
        val action = decidePolicyForNavigationAction
        val target = action.request.URL?.absoluteString.orEmpty()
        val link = noticesLink(target, url, projectPage, allowSameOriginNavigation)
        val mainFrame = action.targetFrame?.isMainFrame() != false
        if (link == NoticesLink.InPage && action.targetFrame != null) {
          if (mainFrame) state.currentUrl = target
          decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
        } else {
          decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
          if (mainFrame && action.navigationType == WKNavigationTypeLinkActivated) {
            when (link) {
              NoticesLink.InPage -> webView.loadRequest(action.request)
              is NoticesLink.Detail -> openDetail.value(
                OnlineNoticeDetailDestination(anchor = link.anchor), webView.scrollView.contentOffset.useContents { y },
              )
              is NoticesLink.Project -> openDetail.value(
                OnlineNoticeDetailDestination(projectUrl = link.url), webView.scrollView.contentOffset.useContents { y },
              )
              NoticesLink.Blocked -> Unit
            }
          }
        }
      }
    }
  }
  val assetBridge = remember(state) { retained?.bridge ?: if (projectPage) null else WebAssetBridge(url) }
  UIKitView(
    modifier = modifier.background(MaterialTheme.colorScheme.surface).deviceViewportGeometry(geometrySource),
    factory = {
      val configuration = WKWebViewConfiguration()
      if (retained == null) assetBridge?.install(configuration.userContentController)
      (retained?.view ?: WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = configuration)).apply {
        bridgeView = this
        hidden = state.loading
        opaque = false
        applyNoticesBackground(surfaceColor)
        scrollView.contentInsetAdjustmentBehavior =
          UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
        navigationDelegate = delegate
        state.reload = { reload() }
        val startUrl = state.currentUrl
        val requestUrl = NSURL.URLWithString(startUrl)
        val initialLink = noticesLink(startUrl, url, projectPage, allowSameOriginNavigation)
        if (retained == null) {
          resourceOwner?.resource = IosNoticesWebResource(this, assetBridge, publisher)
          if (requestUrl != null && initialLink == NoticesLink.InPage) {
            loadRequest(NSURLRequest(uRL = requestUrl))
          } else {
            state.fail()
          }
        }
      }
    },
    update = { view ->
      view.applyNoticesBackground(surfaceColor)
      val inset = UIEdgeInsetsMake(0.0, 0.0, bottomInset.value.toDouble(), 0.0)
      view.scrollView.contentInset = inset
      view.scrollView.scrollIndicatorInsets = inset
    },
    onRelease = {
      delegate.cancelFrameWait()
      state.reload = null
      it.navigationDelegate = null
      if (resourceOwner == null) {
        assetBridge?.close(it.configuration.userContentController)
        it.stopLoading()
      }
    },
  )
}

private fun WKWebView.applyNoticesBackground(color: UIColor) {
  backgroundColor = color
  scrollView.backgroundColor = color
  underPageBackgroundColor = color
}


@OptIn(ExperimentalForeignApi::class)
private class IosNoticesWebResource(
  val view: WKWebView,
  val bridge: WebAssetBridge?,
  val publisher: DeviceViewportPublisher,
) : NoticesWebResource {
  override fun release() {
    view.navigationDelegate = null
    view.stopLoading()
    bridge?.close(view.configuration.userContentController)
  }
}

@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
private class DeviceViewportFrameTarget(private val onFrame: () -> Unit) : NSObject() {
  @ObjCAction
  fun frame() {
    onFrame()
  }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun BindIosDeviceViewport(
  bridgeView: WKWebView?,
  projectPage: Boolean,
  state: NoticesWebState,
  publisher: DeviceViewportPublisher,
  geometrySource: DeviceViewportGeometrySource?,
) {
  DisposableEffect(bridgeView, projectPage) {
    val view = bridgeView
    val target = DeviceViewportFrameTarget {
      val ready = !state.loading && !state.failed
      if (view != null && ready && geometrySource != null) {
        val geometry = geometrySource.snapshot()
        publisher.publish(state.currentUrl, geometry.features, geometry.availability, geometry.key)?.let {
          view.evaluateJavaScript(it, null)
        }
      }
    }
    val link = if (!projectPage && view != null) {
      CADisplayLink.displayLinkWithTarget(target, NSSelectorFromString("frame"))
        .apply { addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes) }
    } else {
      null
    }
    onDispose {
      link?.invalidate()
      if (!projectPage) publisher.detachScript()?.let { view?.evaluateJavaScript(it, null) }
      publisher.reset()
    }
  }
}

@OptIn(ExperimentalForeignApi::class)
private fun WKWebView.bindDeviceViewport(
  publisher: DeviceViewportPublisher,
  source: DeviceViewportGeometrySource?,
  isCurrent: () -> Boolean,
) {
  evaluateJavaScript(publisher.documentScript()) { value, error ->
    if (isCurrent() && error == null) {
      (value as? String)?.let { id ->
        publisher.bindDocument(id)
        val geometry = source?.snapshot() ?: DeviceViewportGeometry()
        publisher.publish(URL?.absoluteString.orEmpty(), geometry.features, geometry.availability, geometry.key)?.let {
          evaluateJavaScript(it, null)
        }
      }
    }
  }
}
