package me.zhangls.entry

import androidx.compose.ui.platform.UriHandler
import platform.UIKit.UIViewController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IosBrowserUriHandlerTest {
  @Test fun detachedHostDoesNotPresentBrowser() {
    val handler = IosBrowserUriHandler(UIViewController(), fallback())
    assertFailsWith<IllegalArgumentException> { handler.openUri("https://example.com") }
    handler.close()
  }

  @Test fun nonWebLinksUseOriginalHandlerWithoutRequiringAWindow() {
    var opened: String? = null
    val fallback = object : UriHandler {
      override fun openUri(uri: String) {
        opened = uri
      }
    }
    val handler = IosBrowserUriHandler(UIViewController(), fallback)
    handler.openUri("mailto:hello@example.com")
    assertEquals("mailto:hello@example.com", opened)
    handler.close()
  }

  private fun fallback() = object : UriHandler {
    override fun openUri(uri: String) = error("Unexpected fallback")
  }
}
