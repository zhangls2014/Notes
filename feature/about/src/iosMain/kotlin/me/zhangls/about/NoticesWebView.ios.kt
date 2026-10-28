package me.zhangls.about

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.about.api.OpenSourcePage
import notes.feature.about.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLErrorCancelled
import platform.Foundation.NSURLQueryItem
import platform.UIKit.UIColor
import platform.UIKit.UIScrollView
import platform.UIKit.UIScrollViewDelegateProtocol
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalResourceApi::class, ExperimentalForeignApi::class)
@Composable
internal actual fun NoticesWebView(
  page: OpenSourcePage,
  anchor: String?,
  appInfo: AboutAppInfo,
  onNavigate: (OpenSourcePage, String?) -> Unit,
  onExternalLink: (String) -> Unit,
  onError: () -> Unit,
  modifier: Modifier,
) {
  val libraries = Res.getUri("files/open-source/ios.html")
  val licenses = Res.getUri("files/open-source/ios-licenses.html")
  val currentPage = rememberUpdatedState(page)
  val navigate = rememberUpdatedState(onNavigate)
  val external = rememberUpdatedState(onExternalLink)
  val failed = rememberUpdatedState(onError)
  val savedY = rememberSaveable { mutableDoubleStateOf(0.0) }
  val browser = remember(libraries, licenses) {
    IosNoticesBrowser(libraries, onScroll = { savedY.doubleValue = it }) { target ->
      val local = localNoticesLink(target, libraries, licenses)
      if (local != null && local.page == currentPage.value) {
        true
      } else {
        if (local != null) {
          navigate.value(local.page, local.anchor)
        } else if (isExternalNoticesLink(target)) {
          external.value(target)
        }
        false
      }
    }
  }
  browser.onError = { failed.value() }
  val components = NSURLComponents(string = if (page == OpenSourcePage.LIBRARIES) libraries else licenses)
  components.queryItems = appInfo.noticesAppearanceParameters().map { (name, value) ->
    NSURLQueryItem(name = name, value = value)
  }
  components.fragment = anchor
  val url = requireNotNull(components.URL)
  UIKitView(
    factory = { browser.view },
    modifier = modifier,
    update = { browser.load(url, savedY.doubleValue) },
    onRelease = { browser.close() },
  )
}

@OptIn(ExperimentalForeignApi::class)
private class IosNoticesBrowser(
  libraries: String,
  private val onScroll: (Double) -> Unit,
  private val allowNavigation: (String) -> Boolean,
) {
  val view = WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = WKWebViewConfiguration())
  private val directory = requireNotNull(NSURL.URLWithString(libraries)?.URLByDeletingLastPathComponent)
  private var loadedUrl: String? = null
  private var loading = true
  private var pendingY = 0.0
  var onError: () -> Unit = {}
  private val delegate = object : NSObject(), WKNavigationDelegateProtocol {
    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
      if (loading && pendingY > 0.0) {
        webView.scrollView.setContentOffset(CGPointMake(0.0, pendingY), animated = false)
      }
      loading = false
    }

    override fun webView(
      webView: WKWebView,
      decidePolicyForNavigationAction: WKNavigationAction,
      decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
      val target = decidePolicyForNavigationAction.request.URL?.absoluteString.orEmpty()
      val allowed = allowNavigation(target)
      decisionHandler(
        if (allowed) {
          WKNavigationActionPolicy.WKNavigationActionPolicyAllow
        } else {
          WKNavigationActionPolicy.WKNavigationActionPolicyCancel
        },
      )
    }

    override fun webView(webView: WKWebView, didFailProvisionalNavigation: WKNavigation?, withError: NSError) {
      if (withError.code != NSURLErrorCancelled) onError()
    }
  }
  private val scrollDelegate = object : NSObject(), UIScrollViewDelegateProtocol {
    override fun scrollViewDidScroll(scrollView: UIScrollView) {
      if (!loading) onScroll(scrollView.contentOffset.useContents { y })
    }
  }

  init {
    view.opaque = false
    view.backgroundColor = UIColor.clearColor
    view.navigationDelegate = delegate
    view.scrollView.delegate = scrollDelegate
  }

  fun load(url: NSURL, savedY: Double) {
    if (loadedUrl != url.absoluteString) {
      pendingY = savedY
      loading = true
      loadedUrl = url.absoluteString
      view.loadFileURL(url, allowingReadAccessToURL = directory)
    }
  }

  fun close() {
    view.stopLoading()
    view.navigationDelegate = null
    view.scrollView.delegate = null
  }
}
