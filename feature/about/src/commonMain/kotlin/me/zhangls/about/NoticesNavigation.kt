package me.zhangls.about

import me.zhangls.about.api.OpenSourcePage

internal data class LocalNoticesLink(val page: OpenSourcePage, val anchor: String?)

private val noticeDetailId = Regex("(?:license-[A-Za-z0-9][A-Za-z0-9.+-]*|notice-[a-f0-9]{16})")

/** Detail identifiers select one declaration in the fixed platform detail page. */
internal fun noticesPageUri(librariesUri: String, page: OpenSourcePage, detailId: String?): String {
  if (page == OpenSourcePage.LIBRARIES) return librariesUri
  require(detailId != null && noticeDetailId.matches(detailId) && !detailId.contains(".."))
  return librariesUri.removeSuffix(".html") + "-licenses.html#$detailId"
}

/** Accept only the exact list URI or fixed detail page with a safe declaration identifier in its directory. */
internal fun localNoticesLink(url: String, librariesUri: String): LocalNoticesLink? {
  val path = url.substringBefore('?').substringBefore('#')
  val details = librariesUri.removeSuffix(".html") + "-licenses.html"
  val detailId = url.substringAfter('#', "")
  return when {
    path == librariesUri -> LocalNoticesLink(OpenSourcePage.LIBRARIES, null)

    path == details &&
      noticeDetailId.matches(detailId) && !detailId.contains("..") -> LocalNoticesLink(
      OpenSourcePage.LICENSES,
      detailId,
    )

    else -> null
  }
}

internal fun isExternalNoticesLink(url: String): Boolean =
  url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)
