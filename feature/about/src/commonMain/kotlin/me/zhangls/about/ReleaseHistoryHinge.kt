package me.zhangls.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.material3.adaptive.Posture
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

internal val Posture.isFullyOpen: Boolean
  get() = hingeList.isNotEmpty() && hingeList.all { it.isFlat }

internal data class ReleaseHistoryLane(val offset: Int, val width: Int)

internal fun releaseHistoryLanes(
  contentBounds: Rect,
  excludedBounds: List<Rect>,
  spacing: Int,
  direction: LayoutDirection,
): List<ReleaseHistoryLane> {
  val width = contentBounds.width.roundToInt()
  val cuts = excludedBounds.filter {
    it.left > contentBounds.left && it.right < contentBounds.right &&
      it.bottom > contentBounds.top && it.top < contentBounds.bottom
  }.sortedBy { it.left }
  val lanes = buildList {
    var start = 0
    for (cut in cuts) {
      val end = (cut.left - contentBounds.left).roundToInt() - spacing / 2
      if (end > start) add(ReleaseHistoryLane(start, end - start))
      start = (cut.right - contentBounds.left).roundToInt() + spacing - spacing / 2
    }
    if (start < width) add(ReleaseHistoryLane(start, width - start))
  }
  return if (direction == LayoutDirection.Rtl) {
    lanes.reversed().map { ReleaseHistoryLane(width - it.offset - it.width, it.width) }
  } else {
    lanes
  }
}

internal class ReleaseHistoryHingeColumns(
  private val viewport: Rect,
  private val excludedBounds: List<Rect>,
  private val padding: Dp,
  override val spacing: Dp,
  private val direction: LayoutDirection,
) : StaggeredGridCells, Arrangement.Horizontal {
  override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): IntArray =
    lanes(availableSize).map { it.width }.toIntArray()

  override fun Density.arrange(
    totalSize: Int,
    sizes: IntArray,
    layoutDirection: LayoutDirection,
    outPositions: IntArray,
  ) {
    lanes(totalSize).forEachIndexed { index, lane -> outPositions[index] = lane.offset }
  }

  private fun Density.lanes(availableSize: Int): List<ReleaseHistoryLane> {
    val left = viewport.left + padding.roundToPx()
    return releaseHistoryLanes(
      Rect(left, viewport.top, left + availableSize, viewport.bottom),
      excludedBounds,
      spacing.roundToPx(),
      direction,
    )
  }
}
