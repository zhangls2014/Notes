package me.zhangls.about

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class AvailableUpdateTest {
  @Test
  fun androidUsesBuildNumberAndRequiresMatchingDownloadPath() {
    val url = "https://notes.zhangls.me/downloads/android/android-prd-full/10/"
    val manifest = UpdateManifest("android-prd-full", "1.10.0", "10", url)
    assertEquals(manifest, availableUpdate(manifest, manifest.channel, "1.10.0", "9"))
    assertNull(availableUpdate(manifest, manifest.channel, "1.10.0", "10"))
    assertFailsWith<IllegalArgumentException> {
      availableUpdate(manifest, "android-prd-mainland", "1.9.0", "9")
    }
    assertFailsWith<IllegalArgumentException> {
      availableUpdate(manifest.copy(updateUrl = "https://example.com/"), manifest.channel, "1.9.0", "9")
    }
  }

  @Test
  fun iosComparesNumericVersionsAndDottedBuilds() {
    val manifest = UpdateManifest("ios-app-store", "1.10.0", "2.10", "https://apps.apple.com/app/id123")
    assertEquals(manifest, availableUpdate(manifest, manifest.channel, "1.9.0", "30"))
    assertEquals(manifest, availableUpdate(manifest, manifest.channel, "1.10.0", "2.9"))
    assertNull(availableUpdate(manifest, manifest.channel, "1.10.0", "2.10.0"))
    assertNull(availableUpdate(manifest, manifest.channel, "1.11.0", "1"))
    assertFailsWith<IllegalArgumentException> {
      availableUpdate(manifest.copy(build = "bad"), manifest.channel, "1.9.0", "1")
    }
  }
}
