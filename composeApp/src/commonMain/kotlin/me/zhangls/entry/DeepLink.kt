package me.zhangls.entry

import me.zhangls.email.detail.EmailDetailDestination
import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.deeplink.parseDeepLinkUrl
import me.zhangls.main.api.HomeDestination

private const val HTTPS = "https"
private const val NOTES_HOST = "notes.zhangls.me"
private const val PATH_HOME = "/home"
private const val PATH_EMAIL = "/email"

fun parseDeepLink(url: String?): DeepLinkDestination? {
  val request = parseDeepLinkUrl(url) ?: return null
  if (request.scheme != HTTPS) return null
  if (request.host != NOTES_HOST) return null

  return when (request.path) {
    PATH_HOME -> HomeDestination
    PATH_EMAIL -> {
      val emailId = request.queries["id"]?.toLongOrNull() ?: return null
      EmailDetailDestination(emailId = emailId)
    }

    else -> null
  }
}
