package me.zhangls.about

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.Dp
import me.zhangls.about.api.OnlineNoticeDetailDestination
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.cinterop.ObjCSignatureOverride
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLErrorCancelled
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
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

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
  val openDetail = rememberUpdatedState(onOpenDetail)
  val delegate = remember(state) {
    object : NSObject(), WKNavigationDelegateProtocol {
      @ObjCSignatureOverride
      override fun webView(webView: WKWebView, didStartProvisionalNavigation: WKNavigation?) {
        state.loading = true
      }

      private var restoreScroll = initialScroll

      @ObjCSignatureOverride
      override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        if (restoreScroll > 0) {
          webView.scrollView.setContentOffset(CGPointMake(0.0, restoreScroll), animated = false)
          restoreScroll = 0.0
        }
        state.loading = false
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
  val assetBridge = remember(state) { if (projectPage) null else WebAssetBridge(url) }
  UIKitView(
    modifier = modifier,
    factory = {
      val configuration = WKWebViewConfiguration()
      assetBridge?.install(configuration.userContentController)
      WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = configuration).apply {
        opaque = false
        applyNoticesBackground(surfaceColor)
        scrollView.contentInsetAdjustmentBehavior =
          UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
        navigationDelegate = delegate
        state.reload = { reload() }
        val startUrl = state.currentUrl
        val requestUrl = NSURL.URLWithString(startUrl)
        val initialLink = noticesLink(startUrl, url, projectPage, allowSameOriginNavigation)
        if (requestUrl != null && initialLink == NoticesLink.InPage) {
          loadRequest(NSURLRequest(uRL = requestUrl))
        } else {
          state.fail()
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
      state.reload = null
      assetBridge?.close(it.configuration.userContentController)
      it.navigationDelegate = null
      it.stopLoading()
    },
  )
}

private fun WKWebView.applyNoticesBackground(color: UIColor) {
  backgroundColor = color
  scrollView.backgroundColor = color
  underPageBackgroundColor = color
}
