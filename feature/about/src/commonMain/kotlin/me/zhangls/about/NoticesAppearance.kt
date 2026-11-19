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
    putAll(
      mapOf(
        "container" to colors.primaryContainer,
        "on-container" to colors.onPrimaryContainer,
        "on-primary" to colors.onPrimary,
        "secondary" to colors.secondary,
        "on-secondary" to colors.onSecondary,
        "secondary-container" to colors.secondaryContainer,
        "on-secondary-container" to colors.onSecondaryContainer,
        "tertiary" to colors.tertiary,
        "on-tertiary" to colors.onTertiary,
        "tertiary-container" to colors.tertiaryContainer,
        "on-tertiary-container" to colors.onTertiaryContainer,
        "surface-container-lowest" to colors.surfaceContainerLowest,
        "surface-container" to colors.surfaceContainer,
        "surface-container-high" to colors.surfaceContainerHigh,
        "surface-container-highest" to colors.surfaceContainerHighest,
        "inverse-surface" to colors.inverseSurface,
        "inverse-on-surface" to colors.inverseOnSurface,
        "inverse-primary" to colors.inversePrimary,
        "surface-tint" to colors.surfaceTint,
        "outline" to colors.border,
      ).filterValues { it != null }.mapValues { checkNotNull(it.value) },
    )
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
  val onPrimary: String? = null,
  val secondary: String? = null,
  val onSecondary: String? = null,
  val secondaryContainer: String? = null,
  val onSecondaryContainer: String? = null,
  val tertiary: String? = null,
  val onTertiary: String? = null,
  val tertiaryContainer: String? = null,
  val onTertiaryContainer: String? = null,
  val surfaceContainerLowest: String? = null,
  val surfaceContainer: String? = null,
  val surfaceContainerHigh: String? = null,
  val surfaceContainerHighest: String? = null,
  val border: String? = null,
  val inverseSurface: String? = null,
  val inverseOnSurface: String? = null,
  val inversePrimary: String? = null,
  val surfaceTint: String? = null,
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
  onPrimary = onPrimary.toRgbHex(),
  secondary = secondary.toRgbHex(),
  onSecondary = onSecondary.toRgbHex(),
  secondaryContainer = secondaryContainer.toRgbHex(),
  onSecondaryContainer = onSecondaryContainer.toRgbHex(),
  tertiary = tertiary.toRgbHex(),
  onTertiary = onTertiary.toRgbHex(),
  tertiaryContainer = tertiaryContainer.toRgbHex(),
  onTertiaryContainer = onTertiaryContainer.toRgbHex(),
  surfaceContainerLowest = surfaceContainerLowest.toRgbHex(),
  surfaceContainer = surfaceContainer.toRgbHex(),
  surfaceContainerHigh = surfaceContainerHigh.toRgbHex(),
  surfaceContainerHighest = surfaceContainerHighest.toRgbHex(),
  border = outline.toRgbHex(),
  inverseSurface = inverseSurface.toRgbHex(),
  inverseOnSurface = inverseOnSurface.toRgbHex(),
  inversePrimary = inversePrimary.toRgbHex(),
  surfaceTint = surfaceTint.toRgbHex(),
)

private fun Color.toRgbHex(): String = (toArgb() and RGB_MASK).toString(HEX_RADIX).padStart(RGB_HEX_LENGTH, '0')
