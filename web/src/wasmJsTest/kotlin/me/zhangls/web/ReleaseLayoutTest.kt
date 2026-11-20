package me.zhangls.web

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReleaseLayoutTest {
  @Test fun windowSizeDoesNotOverrideMobilePlatform() {
    assertEquals("android", resolveReleasePlatform("android", "ios"))
    assertEquals("ios", resolveReleasePlatform("ios", "android"))
    assertEquals("ios", resolveReleasePlatform(null, "ios"))
  }

  @Test fun narrowAndLargeTextUseContinuousReading() {
    assertFalse(releaseUsesTwoPanes(390f, 1f, emptyList()))
    assertTrue(releaseUsesTwoPanes(900f, 1f, emptyList()))
    assertFalse(releaseUsesTwoPanes(900f, 1.5f, emptyList()))
  }

  @Test fun physicalSplitNeedsReadableSpaceOnBothSides() {
    assertTrue(releaseUsesTwoPanes(900f, 1f, listOf(Rect(440f, 0f, 460f, 900f))))
    assertFalse(releaseUsesTwoPanes(900f, 1f, listOf(Rect(700f, 0f, 720f, 900f))))
    assertTrue(releaseUsesTwoPanes(900f, 1f, listOf(Rect(0f, 400f, 900f, 420f))))
  }
}
