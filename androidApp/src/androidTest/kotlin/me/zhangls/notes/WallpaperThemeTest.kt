package me.zhangls.notes

import android.app.WallpaperManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WallpaperThemeTest {
  @Test
  fun onlyExplicitPresetUsesSystemColorsOnSupportedAndroid() {
    assertEquals("preset", readThemeColorSource(31) { config("preset") })
    assertEquals("home_wallpaper", readThemeColorSource(30) { config("preset") })
    for (value in listOf("home_wallpaper", "lock_wallpaper", "unknown")) {
      assertEquals(value, readThemeColorSource(31) { config(value) })
    }
  }

  @Test
  fun missingMalformedOrUnreadableSettingsUseHomeWallpaper() {
    for (value in listOf(null, "", "{}", "null", "invalid")) {
      assertEquals("home_wallpaper", readThemeColorSource(31) { value })
    }
    assertEquals("home_wallpaper", readThemeColorSource(31) { throw SecurityException() })
  }

  @Test
  fun lockWallpaperFallsBackToHomeWhenUnavailable() {
    val requests = mutableListOf<Int>()
    val color = readThemeWallpaperColor("lock_wallpaper") { which ->
      requests += which
      if (which == WallpaperManager.FLAG_SYSTEM) 123 else null
    }
    assertEquals(123, color)
    assertEquals(listOf(WallpaperManager.FLAG_LOCK, WallpaperManager.FLAG_SYSTEM), requests)
  }

  @Test
  fun availableLockWallpaperDoesNotReadHome() {
    val requests = mutableListOf<Int>()
    val color = readThemeWallpaperColor("lock_wallpaper") {
      requests += it
      456
    }
    assertEquals(456, color)
    assertEquals(listOf(WallpaperManager.FLAG_LOCK), requests)
  }

  @Test
  fun homeAndUnknownSourcesOnlyReadHomeWallpaper() {
    for (source in listOf("home_wallpaper", "unknown")) {
      val requests = mutableListOf<Int>()
      val color = readThemeWallpaperColor(source) { which ->
        requests += which
        123
      }
      assertEquals(123, color)
      assertEquals(listOf(WallpaperManager.FLAG_SYSTEM), requests)
    }
  }

  @Test
  fun inaccessibleLockWallpaperStillReadsHome() {
    val color = readThemeWallpaperColor("lock_wallpaper") {
      if (it == WallpaperManager.FLAG_LOCK) throw SecurityException()
      123
    }
    assertEquals(123, color)
    assertEquals(null, readThemeWallpaperColor("home_wallpaper") { throw SecurityException() })
  }

  private fun config(source: String): String = """{"android.theme.customization.color_source":"$source"}"""
}
