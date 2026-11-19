package me.zhangls.about

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class NoticesAppearanceTest {
  @Test fun resolvedThemeKeepsButtonAndCardContrastColors() {
    val scheme = lightColorScheme(
      primary = Color(0xFF123456),
      onPrimary = Color(0xFFFEDCBA),
      secondaryContainer = Color(0xFF654321),
      onSecondaryContainer = Color(0xFFABCDEF),
      tertiaryContainer = Color(0xFF234567),
      onTertiaryContainer = Color(0xFFBCDEFA),
    )
    val parameters = NoticesAppearance(pageColors = scheme.toNoticesColors()).noticesAppearanceParameters()
    assertEquals("123456", parameters["accent"])
    assertEquals("fedcba", parameters["on-primary"])
    assertEquals("654321", parameters["secondary-container"])
    assertEquals("abcdef", parameters["on-secondary-container"])
    assertEquals("234567", parameters["tertiary-container"])
    assertEquals("bcdefa", parameters["on-tertiary-container"])
  }

  @Test fun appLanguageIsPassedToBothPlatforms() {
    assertEquals("zh-CN", NoticesAppearance(language = "zh-CN").noticesAppearanceParameters()["lang"])
    assertEquals("en", NoticesAppearance(language = "en").noticesAppearanceParameters()["lang"])
  }

  @Test fun standalonePagesKeepTheirFallbackPalette() {
    assertEquals(
      mapOf("fontScale" to "1.0", "theme" to "light"),
      NoticesAppearance().noticesAppearanceParameters(),
    )
  }

  @Test fun bothPlatformsReceiveAllResolvedAppearanceValues() {
    val info = NoticesAppearance(
      fontScale = 1.5f,
      darkTheme = true,
      pageColors = NoticesColors(
        background = "151713",
        card = "1e211b",
        foreground = "e4e7dd",
        muted = "b4baaa",
        outline = "373e31",
        accent = "aad395",
        primaryContainer = "354e16",
        onPrimaryContainer = "cdeda3",
      ),
    )
    assertEquals(
      mapOf(
        "fontScale" to "1.5",
        "theme" to "dark",
        "bg" to "151713",
        "card" to "1e211b",
        "fg" to "e4e7dd",
        "muted" to "b4baaa",
        "line" to "373e31",
        "accent" to "aad395",
        "container" to "354e16",
        "on-container" to "cdeda3",
      ),
      info.noticesAppearanceParameters(),
    )
  }
}
