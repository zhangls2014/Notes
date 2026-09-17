package me.zhangls.framework.deeplink

/**
 * 解析后的 DeepLink 请求结构。
 */
data class DeepLinkRequest(
  val scheme: String,
  val host: String,
  val path: String,
  val queries: Map<String, String>,
)

/**
 * 解析 DeepLink URL 为结构化请求。
 *
 * 仅做纯字符串解析，不依赖具体 [Destination] 实现，
 * 因此可以放在 framework 层供组合根复用。
 */
fun parseDeepLinkUrl(url: String?): DeepLinkRequest? {
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
