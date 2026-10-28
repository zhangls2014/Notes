package me.zhangls.entry

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.UriHandler
import io.ktor.http.URLParserException
import io.ktor.http.Url

@Composable
internal expect fun rememberBrowserUriHandler(): UriHandler

/** Keeps non-web schemes on the platform handler and reports launch failures to the caller. */
internal class BrowserUriHandler(private val fallback: UriHandler, private val openWebUri: (String) -> Unit) :
  UriHandler {
  override fun openUri(uri: String) {
    require(uri.isNotBlank()) { "URI must not be blank" }
    val scheme = uri.substringBefore(':').lowercase()
    if (scheme != "http" && scheme != "https") {
      fallback.openUri(uri)
      return
    }
    require(uri.startsWith("$scheme://", ignoreCase = true)) { "Web URI must include a host" }
    require(uri.none { it.isWhitespace() || it.isISOControl() }) { "Web URI contains invalid characters" }
    val authority = uri.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
    require(authority.isNotBlank()) { "Web URI must include a host" }
    val url = try {
      Url(uri)
    } catch (exception: URLParserException) {
      throw IllegalArgumentException("Invalid web URI", exception)
    }
    require(url.host.isNotBlank()) { "Web URI must include a host" }
    openWebUri(uri)
  }
}
