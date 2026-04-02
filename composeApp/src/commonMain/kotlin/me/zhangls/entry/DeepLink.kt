package me.zhangls.entry

import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.main.MainDestination
import me.zhangls.main.detail.EmailDetailDestination

private const val HTTPS = "https"
private const val NOTES_HOST = "notes.zhangls.me"
private const val PATH_HOME = "/home"
private const val PATH_EMAIL = "/email"


fun parseDeepLink(url: String?): DeepLinkDestination? {
  val request = parseDeepLinkUrl(url) ?: return null
  if (request.scheme != HTTPS) return null
  if (request.host != NOTES_HOST) return null

  return when (request.path) {
    PATH_HOME -> MainDestination
    PATH_EMAIL -> {
      val emailId = request.queries["id"]?.toLongOrNull() ?: return null
      EmailDetailDestination(emailId = emailId)
    }

    else -> null
  }
}

private data class DeepLinkRequest(
  val scheme: String,
  val host: String,
  val path: String,
  val queries: Map<String, String>,
)

private fun parseDeepLinkUrl(url: String?): DeepLinkRequest? {
  val raw = url?.trim().takeUnless { it.isNullOrBlank() } ?: return null
  val schemeSeparator = raw.indexOf("://")
  if (schemeSeparator <= 0) return null

  val scheme = raw.substring(0, schemeSeparator).lowercase()
  val authorityAndPath = raw.substring(schemeSeparator + 3)
  if (authorityAndPath.isBlank()) return null

  val slashIndex = authorityAndPath.indexOf('/')
  val authority = if (slashIndex >= 0) authorityAndPath.substring(0, slashIndex) else authorityAndPath
  val pathAndQuery = if (slashIndex >= 0) authorityAndPath.substring(slashIndex) else "/"

  val host = authority.substringBefore(':').lowercase()
  if (host.isBlank()) return null

  val queryIndex = pathAndQuery.indexOf('?')
  val path = (if (queryIndex >= 0) pathAndQuery.substring(0, queryIndex) else pathAndQuery).ifBlank { "/" }
  val query = if (queryIndex >= 0) pathAndQuery.substring(queryIndex + 1) else ""

  return DeepLinkRequest(
    scheme = scheme,
    host = host,
    path = path,
    queries = parseQuery(query),
  )
}

private fun parseQuery(query: String): Map<String, String> {
  if (query.isBlank()) return emptyMap()
  return query
    .split('&')
    .asSequence()
    .filter { it.isNotBlank() }
    .associate { part ->
      val pair = part.split('=', limit = 2)
      val key = pair[0]
      val value = pair.getOrElse(1) { "" }
      key to value
    }
}
