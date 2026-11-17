package me.zhangls.web.notices

import me.zhangls.web.siteTarget
import me.zhangls.web.appearanceColor

// Older app versions supply their native palette without an embed flag.
internal fun hasLegacyNoticePalette(): Boolean = listOf("bg", "card", "fg").all { appearanceColor(it) != null }

internal fun noticeDetailLink(platform: String, id: String): String =
  siteTarget("/open-source/$platform-licenses.html#$id")
