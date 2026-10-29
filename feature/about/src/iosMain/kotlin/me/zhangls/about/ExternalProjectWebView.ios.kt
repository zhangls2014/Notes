package me.zhangls.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLRequest
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIScrollViewContentInsetAdjustmentBehavior
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
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
  val browser = remember {
    ExternalProjectBrowser({ loading.value(it) }, { failed.value() }, { title.value(it) })
  }
  UIKitView(
    factory = { browser.view },
    modifier = modifier,
    update = {
      browser.view.scrollView.contentInset = UIEdgeInsetsMake(0.0, 0.0, bottomInset.value.toDouble(), 0.0)
      browser.load(url)
    },
    onRelease = { browser.close() },
  )
}

@OptIn(ExperimentalForeignApi::class)
private class ExternalProjectBrowser(
  private val onLoading: (Boolean) -> Unit,
  private val onError: () -> Unit,
  private val onTitle: (String?) -> Unit,
) {
  val view = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = WKWebViewConfiguration())
  private var loadedUrl: String? = null
  private val delegate = object : NSObject(), WKNavigationDelegateProtocol {
    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
      onLoading(true)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
      onTitle(webView.title)
      onLoading(false)
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
      if (withError.code != NSURLErrorCancelled) onError()
    }

    @ObjCSignatureOverride
    override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
      if (withError.code != NSURLErrorCancelled) onError()
    }

    override fun webView(
      webView: WKWebView,
      decidePolicyForNavigationAction: WKNavigationAction,
      decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
      val target = decidePolicyForNavigationAction.request.URL?.absoluteString.orEmpty()
      val allowed = isExternalNoticesLink(target)
      decisionHandler(
        if (allowed) {
          WKNavigationActionPolicy.WKNavigationActionPolicyAllow
        } else {
          WKNavigationActionPolicy.WKNavigationActionPolicyCancel
        },
      )
      if (allowed && decidePolicyForNavigationAction.targetFrame == null) {
        webView.loadRequest(decidePolicyForNavigationAction.request)
      }
    }
  }

  init {
    view.scrollView.contentInsetAdjustmentBehavior =
      UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
    view.navigationDelegate = delegate
  }

  fun load(url: String) {
    if (loadedUrl == url) return
    loadedUrl = url
    val target = NSURL.URLWithString(url)
    if (isExternalNoticesLink(url) && target != null) {
      view.loadRequest(NSURLRequest.requestWithURL(target))
    } else {
      onError()
    }
  }

  fun close() {
    view.stopLoading()
    view.navigationDelegate = null
  }
}
