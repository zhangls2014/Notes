package me.zhangls.about

import me.zhangls.about.api.AboutAppInfo
import me.zhangls.about.api.NoticesColors
import kotlin.test.Test
import kotlin.test.assertEquals

class NoticesAppearanceTest {
  @Test fun standalonePagesKeepTheirFallbackPalette() {
    assertEquals(
      mapOf("fontScale" to "1.0", "theme" to "light"),
      AboutAppInfo(null, null).noticesAppearanceParameters(),
    )
  }

  @Test fun bothPlatformsReceiveAllResolvedAppearanceValues() {
    val info = AboutAppInfo(
      versionName = "1.0",
      buildNumber = "1",
      fontScale = 1.5f,
      darkTheme = true,
      pageColors = NoticesColors("151713", "1e211b", "e4e7dd", "b4baaa", "373e31", "aad395"),
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
      ),
      info.noticesAppearanceParameters(),
    )
  }
}
