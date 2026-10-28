package me.zhangls.entry

import androidx.compose.ui.platform.UriHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BrowserUriHandlerTest {
  @Test fun webLinksUseInAppBrowserWithoutChangingQueryOrFragment() {
    val opened = mutableListOf<String>()
    val handler = BrowserUriHandler(fallback = rejectingHandler(), openWebUri = opened::add)
    val links = listOf("https://example.com/path?q=a%20b#section", "HTTP://example.com:8080/")
    links.forEach(handler::openUri)
    assertEquals(links, opened)
  }

  @Test fun otherSchemesKeepTheOriginalSystemHandler() {
    val opened = mutableListOf<String>()
    val fallback = object : UriHandler {
      override fun openUri(uri: String) {
        opened.add(uri)
      }
    }
    val handler = BrowserUriHandler(fallback) { error("Unexpected browser launch") }
    val links = listOf("mailto:hello@example.com", "tel:+123456", "notes://email?id=1")
    links.forEach(handler::openUri)
    assertEquals(links, opened)
  }

  @Test fun invalidWebLinksAreRejectedBeforeLaunching() {
    val handler = BrowserUriHandler(rejectingHandler()) { error("Unexpected browser launch") }
    listOf("", "https:", "https://", "https:///path", "https:// /path", "https://example.com/\npath").forEach { uri ->
      assertFailsWith<IllegalArgumentException>(uri) { handler.openUri(uri) }
    }
  }

  @Test fun launchFailureIsReportedToTheCaller() {
    val handler = BrowserUriHandler(rejectingHandler()) { throw IllegalArgumentException("No browser") }
    assertFailsWith<IllegalArgumentException> { handler.openUri("https://example.com") }
  }

  @Test fun malformedPortUsesTheUriHandlerExceptionContract() {
    val handler = BrowserUriHandler(rejectingHandler()) { error("Unexpected browser launch") }
    assertFailsWith<IllegalArgumentException> { handler.openUri("https://example.com:invalid/path") }
  }

  private fun rejectingHandler() = object : UriHandler {
    override fun openUri(uri: String): Unit = throw IllegalArgumentException("Unsupported URI")
  }
}
