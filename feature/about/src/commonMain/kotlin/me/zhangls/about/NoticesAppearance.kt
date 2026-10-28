package me.zhangls.about

import me.zhangls.about.api.AboutAppInfo

internal fun AboutAppInfo.noticesAppearanceParameters(): Map<String, String> = buildMap {
  put("fontScale", fontScale.toString())
  put("theme", if (darkTheme) "dark" else "light")
  pageColors?.let { colors ->
    put("bg", colors.background)
    put("card", colors.card)
    put("fg", colors.foreground)
    put("muted", colors.muted)
    put("line", colors.outline)
    put("accent", colors.accent)
    colors.primaryContainer?.let { put("container", it) }
    colors.onPrimaryContainer?.let { put("on-container", it) }
  }
}
