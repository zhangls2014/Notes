package me.zhangls.entry.util

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.about.api.NoticesColors

private const val RGB_MASK = 0xFFFFFF
private const val HEX_RADIX = 16
private const val RGB_HEX_LENGTH = 6

/** Package metadata is read by the host, never by the feature UI. */
internal fun AppInfo.toAboutAppInfo(
  fontScale: Float = 1f,
  darkTheme: Boolean = false,
  pageColors: NoticesColors? = null,
): AboutAppInfo = AboutAppInfo(
  versionName = getVersionName().takeIf { it.isNotBlank() },
  buildNumber = getBuildNumber()?.takeIf { it.isNotBlank() },
  fontScale = fontScale,
  darkTheme = darkTheme,
  pageColors = pageColors,
)

internal fun ColorScheme.toNoticesColors(): NoticesColors = NoticesColors(
  background = surface.toRgbHex(),
  card = surfaceContainerLow.toRgbHex(),
  foreground = onSurface.toRgbHex(),
  muted = onSurfaceVariant.toRgbHex(),
  outline = outlineVariant.toRgbHex(),
  accent = primary.toRgbHex(),
  primaryContainer = primaryContainer.toRgbHex(),
  onPrimaryContainer = onPrimaryContainer.toRgbHex(),
)

private fun Color.toRgbHex(): String = (toArgb() and RGB_MASK).toString(HEX_RADIX).padStart(RGB_HEX_LENGTH, '0')
