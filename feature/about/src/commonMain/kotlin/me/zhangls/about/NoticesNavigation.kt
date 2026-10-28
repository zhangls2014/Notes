package me.zhangls.about

import me.zhangls.about.api.OpenSourcePage

internal data class LocalNoticesLink(val page: OpenSourcePage, val anchor: String?)

/** Only the two exact bundled file URIs are internal navigation targets. */
internal fun localNoticesLink(url: String, librariesUri: String, licensesUri: String): LocalNoticesLink? {
  val page = when (url.substringBefore('?').substringBefore('#')) {
    librariesUri -> OpenSourcePage.LIBRARIES
    licensesUri -> OpenSourcePage.LICENSES
    else -> return null
  }
  return LocalNoticesLink(page, url.substringAfter('#', "").takeIf { it.isNotEmpty() })
}

internal fun isExternalNoticesLink(url: String): Boolean =
  url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)
