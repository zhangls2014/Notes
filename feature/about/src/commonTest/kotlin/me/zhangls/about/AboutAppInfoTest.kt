package me.zhangls.about

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import me.zhangls.framework.AppInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AboutAppInfoTest {
  @Test fun usesResolvedMaterialColorsIncludingDynamicSchemes() {
    val schemes = listOf(lightColorScheme(), darkColorScheme()).map { scheme ->
      scheme.copy(
        surface = Color(0xFF010203),
        surfaceContainerLow = Color(0xFF102030),
        onSurface = Color(0xFFAABBCC),
        onSurfaceVariant = Color(0xFF405060),
        outlineVariant = Color(0xFF708090),
        primary = Color(0xFF0011EE),
        primaryContainer = Color(0xFF112233),
        onPrimaryContainer = Color(0xFFDDEEFF),
        onPrimary = Color(0xFF000001),
        secondary = Color(0xFF000002),
        onSecondary = Color(0xFF000003),
        secondaryContainer = Color(0xFF000004),
        onSecondaryContainer = Color(0xFF000005),
        tertiary = Color(0xFF000006),
        onTertiary = Color(0xFF000007),
        tertiaryContainer = Color(0xFF000008),
        onTertiaryContainer = Color(0xFF000009),
        surfaceContainerLowest = Color(0xFF00000A),
        surfaceContainer = Color(0xFF00000B),
        surfaceContainerHigh = Color(0xFF00000C),
        surfaceContainerHighest = Color(0xFF00000D),
        outline = Color(0xFF00000E),
        inverseSurface = Color(0xFF00000F),
        inverseOnSurface = Color(0xFF000010),
        inversePrimary = Color(0xFF000011),
        surfaceTint = Color(0xFF000012),
      )
    }
    for (scheme in schemes) {
      val colors = scheme.toNoticesColors()
      assertEquals(
        expectedResolvedColors(),
        colors,
      )
      assertEquals(colors, NoticesAppearance(pageColors = colors).pageColors)
    }
  }

  private fun expectedResolvedColors() = NoticesColors(
    background = "010203",
    card = "102030",
    foreground = "aabbcc",
    muted = "405060",
    outline = "708090",
    accent = "0011ee",
    primaryContainer = "112233",
    onPrimaryContainer = "ddeeff",
    onPrimary = "000001",
    secondary = "000002",
    onSecondary = "000003",
    secondaryContainer = "000004",
    onSecondaryContainer = "000005",
    tertiary = "000006",
    onTertiary = "000007",
    tertiaryContainer = "000008",
    onTertiaryContainer = "000009",
    surfaceContainerLowest = "00000a",
    surfaceContainer = "00000b",
    surfaceContainerHigh = "00000c",
    surfaceContainerHighest = "00000d",
    border = "00000e",
    inverseSurface = "00000f",
    inverseOnSurface = "000010",
    inversePrimary = "000011",
    surfaceTint = "000012",
  )

  @Test fun noticesKeepCurrentAppearanceWithoutAHostedUrl() {
    for (scale in listOf(1f, 1.5f, 2f)) {
      for (dark in listOf(false, true)) {
        val info = NoticesAppearance(fontScale = scale, darkTheme = dark)
        assertEquals(scale, info.fontScale)
        assertEquals(dark, info.darkTheme)
      }
    }
  }

  @Test fun retainsPlatformVersionAndDottedBuildIdentifier() {
    val info = appInfo("2.3.1", "42.7.1").toAboutAppInfo()
    assertEquals("2.3.1", info.versionName)
    assertEquals("42.7.1", info.buildNumber)
  }

  @Test fun unavailableMetadataDoesNotBecomeAnInventedVersion() {
    val info = appInfo("  ", null).toAboutAppInfo()
    assertNull(info.versionName)
    assertNull(info.buildNumber)
    assertNull(appInfo("", " ").toAboutAppInfo().buildNumber)
  }

  private fun appInfo(version: String, build: String?) = object : AppInfo {
    override fun getVersionName() = version
    override fun getVersionCode() = 0L
    override fun getBuildNumber() = build
  }
}
