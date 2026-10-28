package me.zhangls.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.uikit.LocalUIViewController
import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIViewController

@Composable
internal actual fun rememberBrowserUriHandler(): UriHandler {
  val host = LocalUIViewController.current
  val fallback = LocalUriHandler.current
  val handler = remember(host, fallback) { IosBrowserUriHandler(host, fallback) }
  DisposableEffect(handler) {
    onDispose { handler.close() }
  }
  return handler
}

internal class IosBrowserUriHandler(private val host: UIViewController, fallback: UriHandler) : UriHandler {
  private var browser: SFSafariViewController? = null
  private val handler = BrowserUriHandler(fallback, ::openWebUri)

  override fun openUri(uri: String) = handler.openUri(uri)

  private fun openWebUri(uri: String) {
    val url = requireNotNull(NSURL.URLWithString(uri)) { "Invalid web URI" }
    require(!url.host.isNullOrBlank()) { "Web URI must include a host" }
    require(host.viewIfLoaded?.window != null && host.presentedViewController == null && !host.isBeingDismissed()) {
      "Browser host is not available for presentation"
    }
    val controller = SFSafariViewController(uRL = url)
    browser = controller
    host.presentViewController(controller, animated = true, completion = null)
  }

  fun close() {
    browser?.let { controller ->
      if (host.presentedViewController == controller) {
        controller.dismissViewControllerAnimated(false, completion = null)
      }
    }
    browser = null
  }
}
