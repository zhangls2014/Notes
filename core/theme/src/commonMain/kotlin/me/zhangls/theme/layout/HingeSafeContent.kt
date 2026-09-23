package me.zhangls.theme.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A bounded viewport in the largest physical safe region. Measures at placement time because the
 * window origin is then available, including Rail, status bar and dialog offsets. Reading placement
 * coordinates also makes Compose rerun placement when an ancestor moves: no cached origin, unsafe
 * first frame, or onGloballyPositioned -> state -> next-frame correction.
 *
 * Horizontal-only is for the Nav3 host; the scene scaffold still owns vertical list/detail panes.
 */
@Composable
@Suppress("FunctionSignature") // Ktlint assumes four-space continuation; this project uses two.
fun HingeSafeContent(
  modifier: Modifier = Modifier,
  horizontalOnly: Boolean = false,
  enabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  val hinges = LocalWindowHinges.current.filter {
    enabled && (!horizontalOnly || !it.isVertical)
  }
  Layout(
    content = {
      Box(Modifier.fillMaxSize().clipToBounds()) {
        content()
      }
    },
    modifier = modifier.fillMaxSize().clipToBounds(),
  ) { measurables, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    check(constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
      "HingeSafeContent requires a bounded viewport"
    }
    layout(width, height) {
      val origin = coordinates?.localToWindow(Offset.Zero) ?: return@layout
      val windowBounds = Rect(origin.x, origin.y, origin.x + width, origin.y + height)
      val region = largestSafeRegion(safeRegions(windowBounds, hinges))
        ?: return@layout
      val left = ceil(region.left - origin.x).toInt()
      val top = ceil(region.top - origin.y).toInt()
      val right = floor(region.right - origin.x).toInt()
      val bottom = floor(region.bottom - origin.y).toInt()
      val childConstraints = Constraints.fixed(
        width = (right - left).coerceAtLeast(0),
        height = (bottom - top).coerceAtLeast(0),
      )
      val placeable = measurables.single().measure(childConstraints)
      placeable.place(left, top)
    }
  }
}

/** Login's two physical horizontal bands. If only one fits, the form (second) has priority. */
@Composable
fun HingeSafeColumn(modifier: Modifier = Modifier, first: @Composable () -> Unit, second: @Composable () -> Unit) {
  val hinges = LocalWindowHinges.current
  Layout(
    modifier = modifier.fillMaxSize().clipToBounds(),
    content = {
      Box(Modifier.fillMaxSize().clipToBounds()) { first() }
      Box(Modifier.fillMaxSize().clipToBounds()) { second() }
    },
  ) { measurables, constraints ->
    check(constraints.hasBoundedWidth && constraints.hasBoundedHeight)
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    layout(width, height) {
      val origin = coordinates?.localToWindow(Offset.Zero) ?: return@layout
      val windowBounds = Rect(origin.x, origin.y, origin.x + width, origin.y + height)
      val regions = safeRegions(windowBounds, hinges)
        .sortedByDescending { it.width * it.height }
        .take(2)
        .sortedWith(compareBy({ it.top }, { it.left }))
      regions.forEachIndexed { index, region ->
        val measurable = measurables[if (regions.size == 1) 1 else index]
        val x = ceil(region.left - origin.x).toInt()
        val y = ceil(region.top - origin.y).toInt()
        val right = floor(region.right - origin.x).toInt()
        val bottom = floor(region.bottom - origin.y).toInt()
        val childConstraints = Constraints.fixed(
          width = (right - x).coerceAtLeast(0),
          height = (bottom - y).coerceAtLeast(0),
        )
        measurable.measure(childConstraints).place(x, y)
      }
    }
  }
}
