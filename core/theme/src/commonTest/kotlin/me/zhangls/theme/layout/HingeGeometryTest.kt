package me.zhangls.theme.layout

import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.ui.geometry.Rect
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HingeGeometryTest {
  private val window = Rect(0f, 0f, 1200f, 900f)

  @Test
  fun noHingePreservesOriginalViewportAndDisablesSafeLayout() {
    assertEquals(listOf(window), safeRegions(window, emptyList()))
    assertEquals(emptyList(), safeRegions(Rect.Zero, emptyList()))
    assertFalse(shouldUseHingeLayout(emptyList(), horizontalOnly = false))
    assertFalse(shouldUseHingeLayout(emptyList(), horizontalOnly = true))
  }

  @Test
  fun flatAndHalfOpenKeepEveryVerticalHinge() {
    val half = info(listOf(hinge(Rect(400f, 0f, 410f, 900f)), hinge(Rect(800f, 0f, 800f, 900f))))
    val flat = info(half.windowPosture.hingeList.map { hinge(it.bounds, flat = true) })
    assertEquals(calculateAppPaneScaffoldDirective(half), calculateAppPaneScaffoldDirective(flat))
    assertEquals(2, calculateAppPaneScaffoldDirective(flat).excludedBounds.size)
  }

  @Test
  fun unsortedTriFoldProducesThreeSafeRegions() {
    val hinges = listOf(
      hinge(Rect(800f, 0f, 810f, 900f)),
      hinge(Rect(400f, 0f, 400f, 900f)),
    )
    assertEquals(
      listOf(
        Rect(0f, 0f, 400f, 900f),
        Rect(400f, 0f, 800f, 900f),
        Rect(810f, 0f, 1200f, 900f),
      ),
      safeRegions(window, hinges),
    )
  }

  @Test
  fun horizontalFilteringOnlyEnablesRelevantSafeLayout() {
    val vertical = hinge(Rect(400f, 0f, 410f, 900f))
    val horizontal = hinge(Rect(0f, 200f, 1200f, 230f), vertical = false)
    assertFalse(shouldUseHingeLayout(listOf(vertical), horizontalOnly = true))
    assertTrue(shouldUseHingeLayout(listOf(horizontal), horizontalOnly = true))
    assertTrue(shouldUseHingeLayout(listOf(vertical), horizontalOnly = false))
    assertEquals(
      listOf(Rect(0f, 0f, 1200f, 200f), Rect(0f, 230f, 1200f, 900f)),
      safeRegions(window, listOf(horizontal)),
    )
  }

  @Test
  fun clippingRespectsWindowOriginAndNonIntersectingHinges() {
    val content = Rect(100f, 30f, 700f, 830f)
    val hinges = listOf(
      hinge(Rect(0f, 0f, 110f, 900f)),
      hinge(Rect(450f, 0f, 470f, 900f)),
      hinge(Rect(900f, 0f, 910f, 900f)),
    )
    assertEquals(
      listOf(Rect(110f, 30f, 450f, 830f), Rect(470f, 30f, 700f, 830f)),
      safeRegions(content, hinges),
    )
    assertEquals(listOf(window), safeRegions(window, listOf(hinge(Rect(400f, 1000f, 410f, 1200f)))))
  }

  @Test
  fun overlappingHorizontalHingesNeverRejoinRegions() {
    val hinges = listOf(
      hinge(Rect(0f, 600f, 1200f, 610f), vertical = false),
      hinge(Rect(0f, 300f, 1200f, 320f), vertical = false),
      hinge(Rect(0f, 310f, 1200f, 330f), vertical = false),
    )
    assertEquals(
      listOf(
        Rect(0f, 0f, 1200f, 300f),
        Rect(0f, 330f, 1200f, 600f),
        Rect(0f, 610f, 1200f, 900f),
      ),
      safeRegions(window, hinges),
    )
  }

  private fun info(hinges: List<HingeInfo>) = WindowAdaptiveInfo(
    WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(1200, 900),
    Posture(false, hinges),
  )

  private fun hinge(bounds: Rect, vertical: Boolean = true, flat: Boolean = false) =
    HingeInfo(bounds, flat, vertical, !flat, false)
}
