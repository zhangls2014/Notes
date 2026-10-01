package me.zhangls.entry

import androidx.compose.ui.geometry.Size

internal object PageMotionGeometry {
  fun transform(pageSize: Size, progress: Float, horizontalReduction: Float, verticalReduction: Float): PageMotionTransform {
    val fraction = progress.coerceIn(0f, 1f)
    val scaleX = if (pageSize.width > 0f) {
      1f - minOf(horizontalReduction, pageSize.width) * fraction / pageSize.width
    } else 1f
    val scaleY = if (pageSize.height > 0f) {
      1f - minOf(verticalReduction, pageSize.height) * fraction / pageSize.height
    } else 1f
    // Scale around the page center.
    return PageMotionTransform(scaleX, scaleY,
      pageSize.width / 2f * (1f - scaleX), pageSize.height / 2f * (1f - scaleY))
  }
}

internal data class PageMotionTransform(
  val scaleX: Float,
  val scaleY: Float,
  val translationX: Float,
  val translationY: Float,
)
