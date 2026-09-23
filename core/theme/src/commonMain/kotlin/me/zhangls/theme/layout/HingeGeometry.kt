package me.zhangls.theme.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect

/** Window-pixel geometry, also available to dialogs and previews without a navigation host. */
val LocalWindowHinges = staticCompositionLocalOf<List<HingeInfo>> { emptyList() }

val WindowAdaptiveInfo.hasHinges: Boolean get() = windowPosture.hingeList.isNotEmpty()
val WindowAdaptiveInfo.hasHorizontalHinge: Boolean
  get() = windowPosture.hingeList.any { !it.isVertical }

fun shouldUseHingeLayout(hinges: List<HingeInfo>, horizontalOnly: Boolean): Boolean =
  hinges.any { !horizontalOnly || !it.isVertical }

/** Keep posture facts intact; only layout policy treats a flat fold like a half-open fold. */
fun WindowAdaptiveInfo.forStableLayout(): WindowAdaptiveInfo = WindowAdaptiveInfo(
  windowSizeClass,
  Posture(isTabletop = hasHorizontalHinge, hingeList = windowPosture.hingeList),
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun calculateAppPaneScaffoldDirective(info: WindowAdaptiveInfo): PaneScaffoldDirective {
  val directive = calculatePaneScaffoldDirective(info.forStableLayout(), HingePolicy.AlwaysAvoid)
  val merged = mutableListOf<Rect>()
  directive.excludedBounds.sortedBy { it.left }.forEach { next ->
    val last = merged.lastOrNull()
    if (last != null && next.left <= last.right) {
      merged[merged.lastIndex] = Rect(
        left = last.left,
        top = minOf(last.top, next.top),
        right = maxOf(last.right, next.right),
        bottom = maxOf(last.bottom, next.bottom),
      )
    } else {
      merged.add(next)
    }
  }
  return directive.copy(excludedBounds = merged)
}

/** Split a viewport at every intersecting physical hinge, using window-pixel coordinates. */
fun safeRegions(bounds: Rect, hinges: List<HingeInfo>): List<Rect> {
  if (bounds.width <= 0 || bounds.height <= 0) return emptyList()
  var regions = listOf(bounds)
  hinges.sortedWith(compareBy({ it.bounds.top }, { it.bounds.left })).forEach { hinge ->
    val h = hinge.bounds
    regions = regions.flatMap { region ->
      val horizontalOverlap = h.right > region.left && h.left < region.right
      val verticalOverlap = h.bottom > region.top && h.top < region.bottom
      if (!horizontalOverlap || !verticalOverlap) {
        listOf(region)
      } else if (hinge.isVertical) {
        listOf(
          Rect(region.left, region.top, maxOf(region.left, h.left), region.bottom),
          Rect(minOf(region.right, h.right), region.top, region.right, region.bottom),
        ).filter { it.width > 0 }
      } else {
        listOf(
          Rect(region.left, region.top, region.right, maxOf(region.top, h.top)),
          Rect(region.left, minOf(region.bottom, h.bottom), region.right, region.bottom),
        ).filter { it.height > 0 }
      }
    }
  }
  return regions.sortedWith(compareBy({ it.top }, { it.left }))
}

/** Stable physical order breaks equal-area ties; folding state and reporting order do not. */
fun largestSafeRegion(regions: List<Rect>): Rect? = regions.maxByOrNull { it.width * it.height }
