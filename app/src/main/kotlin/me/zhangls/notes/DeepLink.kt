package me.zhangls.notes

import android.net.Uri
import androidx.core.net.toUri
import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.deeplink.DeepLinkRequest
import me.zhangls.notes.ui.favorites.FavoritesDestination

/**
 * @author zhangls
 */
private const val URL_FAVORITES = "https://notes.zhangls.me/favorites?id=xx&token=xxxx"
private const val URL_HOME = "https://notes.zhangls.me/home"

private val deepLinkPatterns = listOf(
  URL_FAVORITES.toUri()
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
      URL_FAVORITES -> FavoritesDestination
      else -> null
    }
  }
  return null
}