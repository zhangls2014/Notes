package me.zhangls.theme.layout

import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ViewportGeometryTest {
  @Test
  fun clipsAndNormalizesWindowBoundsWithoutLosingFoldLines() {
    val view = Rect(100f, 200f, 900f, 800f)
    assertEquals(Rect(.5f, 0f, .5f, 1f), normalizeViewportFeature(Rect(500f, 0f, 500f, 1000f), view))
    assertEquals(Rect(0f, .5f, 1f, .5f), normalizeViewportFeature(Rect(0f, 500f, 1000f, 500f), view))
    assertEquals(Rect(0f, 0f, .25f, 1f), normalizeViewportFeature(Rect(0f, 0f, 300f, 1000f), view))
  }

  @Test
  fun excludesNonIntersectingAndInvalidGeometry() {
    val view = Rect(100f, 200f, 900f, 800f)
    assertNull(normalizeViewportFeature(Rect(20f, 0f, 20f, 1000f), view))
    assertNull(normalizeViewportFeature(Rect(300f, 300f, 300f, 300f), view))
    assertNull(normalizeViewportFeature(Rect(Float.NaN, 0f, 200f, 300f), view))
    assertNull(normalizeViewportFeature(Rect(300f, 0f, 200f, 300f), view))
    assertNull(normalizeViewportFeature(view, Rect.Zero))
  }
}
