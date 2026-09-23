package me.zhangls.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import me.zhangls.theme.layout.LocalWindowHinges
import me.zhangls.theme.layout.largestSafeRegion
import me.zhangls.theme.layout.safeRegions
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The library navigation components still render their own items. Only their physical placement
 * differs: bottom navigation is inside one safe segment, and a Rail fits the leading segment.
 * The Nav3 content keeps the remaining full span so its scene can use other vertical segments.
 */
@Composable
@Suppress("FunctionSignature") // Ktlint assumes four-space continuation; this project uses two.
internal fun HingeNavigationLayout(
  isRail: Boolean,
  navigation: @Composable () -> Unit,
  content: @Composable () -> Unit,
) {
  val hinges = LocalWindowHinges.current
  Layout(
    modifier = Modifier.fillMaxSize().clipToBounds(),
    content = {
      Box(Modifier.clipToBounds()) { navigation() }
      Box(Modifier.fillMaxSize().clipToBounds()) { content() }
    },
  ) { measurables, constraints ->
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val rtl = layoutDirection == LayoutDirection.Rtl
    layout(width, height) {
      val origin = coordinates?.localToWindow(Offset.Zero) ?: return@layout
      val windowBounds = Rect(origin.x, origin.y, origin.x + width, origin.y + height)
      val regions = safeRegions(windowBounds, hinges)
      val region = if (isRail) {
        if (rtl) {
          regions.maxByOrNull { it.right }
        } else {
          regions.minByOrNull { it.left }
        }
      } else {
        largestSafeRegion(regions)
      }
      if (region == null) return@layout
      val left = ceil(region.left - origin.x).toInt()
      val right = floor(region.right - origin.x).toInt()
      val top = ceil(region.top - origin.y).toInt()
      val bottom = floor(region.bottom - origin.y).toInt()
      val navigationConstraints = Constraints(
        maxWidth = (right - left).coerceAtLeast(0),
        maxHeight = (bottom - top).coerceAtLeast(0),
      )
      val nav = measurables[0].measure(navigationConstraints)
      if (isRail) {
        val navX = if (rtl) right - nav.width else left
        nav.place(navX, top)
        val contentLeft = if (rtl) 0 else navX + nav.width
        val contentRight = if (rtl) navX else width
        val contentConstraints = Constraints.fixed(
          width = (contentRight - contentLeft).coerceAtLeast(0),
          height = height,
        )
        measurables[1].measure(contentConstraints).place(contentLeft, 0)
      } else {
        val navY = bottom - nav.height
        nav.place(left, navY)
        measurables[1].measure(Constraints.fixed(width, navY.coerceAtLeast(0))).place(0, 0)
      }
    }
  }
}
