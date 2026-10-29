package me.zhangls.about

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale
import me.zhangls.theme.LocalAppDarkTheme

internal data class NoticesAppearance(
  val fontScale: Float = 1f,
  val darkTheme: Boolean = false,
  val pageColors: NoticesColors? = null,
  val language: String? = null,
)

@Composable
internal fun currentNoticesAppearance(): NoticesAppearance = NoticesAppearance(
  fontScale = LocalDensity.current.fontScale,
  darkTheme = LocalAppDarkTheme.current,
  pageColors = MaterialTheme.colorScheme.toNoticesColors(),
  language = Locale.current.toLanguageTag(),
)

internal fun NoticesAppearance.noticesAppearanceParameters(): Map<String, String> = buildMap {
  language?.let { put("lang", it) }
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

/** Resolved app colors as six-digit RGB hex strings, without a leading '#'. */
internal data class NoticesColors(
  val background: String,
  val card: String,
  val foreground: String,
  val muted: String,
  val outline: String,
  val accent: String,
  val primaryContainer: String? = null,
  val onPrimaryContainer: String? = null,
)

private const val RGB_MASK = 0xFFFFFF
private const val HEX_RADIX = 16
private const val RGB_HEX_LENGTH = 6

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
