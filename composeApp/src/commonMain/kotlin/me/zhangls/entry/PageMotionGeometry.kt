package me.zhangls.entry

import androidx.compose.ui.geometry.Size

internal object PageMotionGeometry {
  private const val MIN_SCALE = 0.85f

  fun transform(pageSize: Size, progress: Float, anchorX: Float = 0.5f): PageMotionTransform {
    val fraction = progress.coerceIn(0f, 1f)
    val scale = 1f - (1f - MIN_SCALE) * fraction
    // Keep the selected horizontal anchor and vertical center fixed throughout the motion.
    return PageMotionTransform(
      scaleX = scale,
      scaleY = scale,
      translationX = pageSize.width * anchorX * (1f - scale),
      translationY = pageSize.height / 2f * (1f - scale),
    )
  }
}

internal data class PageMotionTransform(
  val scaleX: Float,
  val scaleY: Float,
  val translationX: Float,
  val translationY: Float,
)
