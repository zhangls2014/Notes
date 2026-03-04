package me.zhangls.notes

import android.net.Uri
import androidx.core.net.toUri
import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.deeplink.DeepLinkRequest
import me.zhangls.main.detail.EmailDetailDestination

/**
 * @author zhangls
 */
private const val URL_EMAIL = "https://notes.zhangls.me/email?id=xx&token=xxxx"
private const val URL_HOME = "https://notes.zhangls.me/home"

private val deepLinkPatterns = listOf(
  URL_EMAIL.toUri()
)

internal fun parseDeepLink(uri: Uri?): DeepLinkDestination? {
  return uri?.let {
    val request = DeepLinkRequest(it)

    deepLinkPatterns.firstNotNullOfOrNull { pattern ->
      deepLinkMatcher(request, pattern)
    }
  }
}

private fun deepLinkMatcher(request: DeepLinkRequest, pattern: Uri): DeepLinkDestination? {
  if (request.scheme != pattern.scheme) return null
  if (request.host != pattern.host) return null
  if (request.pathSegments.size != pattern.pathSegments.size) return null

  if (request.path == pattern.path) {
    return when (pattern.toString()) {
      URL_EMAIL -> EmailDetailDestination(emailId = request.queries["id"]?.toLong() ?: return null)
      else -> null
    }
  }
  return null
}