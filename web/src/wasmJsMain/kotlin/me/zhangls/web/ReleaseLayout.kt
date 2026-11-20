package me.zhangls.web

import androidx.compose.ui.geometry.Rect

internal fun resolveReleasePlatform(devicePlatform: String?, requestedPlatform: String?): String =
  devicePlatform ?: requestedPlatform?.takeIf { it == "android" || it == "ios" } ?: "android"

internal fun releaseUsesTwoPanes(width: Float, fontScale: Float, hinges: List<Rect>): Boolean {
  val scale = maxOf(1f, fontScale)
  val vertical = hinges.filter { it.height > it.width && it.left > 0 && it.right < width }.sortedBy { it.left }
  if (vertical.isEmpty()) return width >= 840f * scale
  val segments = buildList {
    var start = 0f
    vertical.forEach { hinge ->
      if (hinge.left > start) add(hinge.left - start)
      start = maxOf(start, hinge.right)
    }
    add(width - start)
  }
  return segments.size >= 2 && segments[0] >= 220f * scale && segments[1] >= 360f * scale
}
