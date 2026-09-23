package me.zhangls.theme.layout

import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.ui.geometry.Rect
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlin.test.Test
import kotlin.test.assertEquals

class HingeGeometryTest {
  @Test
  fun noHingeProducesNoExcludedBounds() {
    assertEquals(emptyList(), calculateAppPaneScaffoldDirective(info(emptyList())).excludedBounds)
  }

  @Test
  fun flatAndHalfOpenKeepEveryVerticalHinge() {
    val half = info(listOf(hinge(Rect(400f, 0f, 410f, 900f)), hinge(Rect(800f, 0f, 800f, 900f))))
    val flat = info(half.windowPosture.hingeList.map { hinge(it.bounds, flat = true) })
    assertEquals(calculateAppPaneScaffoldDirective(half), calculateAppPaneScaffoldDirective(flat))
    assertEquals(2, calculateAppPaneScaffoldDirective(flat).excludedBounds.size)
  }

  @Test
  fun overlappingVerticalHingesAreMergedForPaneLayout() {
    val hinges = listOf(
      hinge(Rect(400f, 0f, 410f, 900f)),
      hinge(Rect(405f, 0f, 420f, 900f)),
    )
    assertEquals(
      listOf(Rect(400f, 0f, 420f, 900f)),
      calculateAppPaneScaffoldDirective(info(hinges)).excludedBounds,
    )
  }

  @Test
  fun flatAndHalfOpenHorizontalHingeKeepPaneDirectiveStable() {
    val bounds = Rect(0f, 440f, 1200f, 460f)
    val half = info(listOf(hinge(bounds, vertical = false)))
    val flat = info(listOf(hinge(bounds, vertical = false, flat = true)))
    assertEquals(
      calculateAppPaneScaffoldDirective(half),
      calculateAppPaneScaffoldDirective(flat),
    )
  }

  private fun info(hinges: List<HingeInfo>) = WindowAdaptiveInfo(
    WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(1200, 900),
    Posture(false, hinges),
  )

  private fun hinge(bounds: Rect, vertical: Boolean = true, flat: Boolean = false) =
    HingeInfo(bounds, flat, vertical, !flat, false)
}
