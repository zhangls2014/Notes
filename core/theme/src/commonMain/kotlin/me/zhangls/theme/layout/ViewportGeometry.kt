package me.zhangls.theme.layout

import androidx.compose.ui.geometry.Rect

/** Clip a window-local feature to a viewport, retaining zero-width/height fold lines. */
fun normalizeViewportFeature(feature: Rect, viewport: Rect): Rect? {
  if (!feature.isValidFeature() || !viewport.isValidViewport()) return null
  val clipped = Rect(
    maxOf(feature.left, viewport.left),
    maxOf(feature.top, viewport.top),
    minOf(feature.right, viewport.right),
    minOf(feature.bottom, viewport.bottom),
  )
  return clipped.takeIf { it.isValidFeature() }?.let {
    Rect(
      (it.left - viewport.left) / viewport.width,
      (it.top - viewport.top) / viewport.height,
      (it.right - viewport.left) / viewport.width,
      (it.bottom - viewport.top) / viewport.height,
    )
  }
}

private fun Rect.isValidFeature(): Boolean = isFinite && width >= 0 && height >= 0 && (width > 0 || height > 0)

private fun Rect.isValidViewport(): Boolean = isFinite && width > 0 && height > 0
