package me.zhangls.theme.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.HingePolicy
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp

/** Treat every physical horizontal hinge consistently, regardless of its reported folding state. */
private fun WindowAdaptiveInfo.forPaneLayout(): WindowAdaptiveInfo = WindowAdaptiveInfo(
  windowSizeClass,
  Posture(
    isTabletop = windowPosture.hingeList.any { !it.isVertical },
    hingeList = windowPosture.hingeList,
  ),
)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun calculateAppPaneScaffoldDirective(info: WindowAdaptiveInfo): PaneScaffoldDirective {
  val directive = calculatePaneScaffoldDirective(info.forPaneLayout(), HingePolicy.AlwaysAvoid)
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
  return directive.copy(
    horizontalPartitionSpacerSize = 0.dp,
    verticalPartitionSpacerSize = 0.dp,
    excludedBounds = merged,
  )
}
