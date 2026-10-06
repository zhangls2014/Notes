package me.zhangls.entry

import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.RoundedCornerCompat
import androidx.core.view.ViewCompat

@Composable
internal actual fun rememberDeviceCorners(): DeviceCorners? {
  val view = LocalView.current
  fun readCorners(): DeviceCorners? {
    val insets = ViewCompat.getRootWindowInsets(view) ?: return null
    val positions = listOf(RoundedCornerCompat.POSITION_TOP_LEFT, RoundedCornerCompat.POSITION_TOP_RIGHT,
      RoundedCornerCompat.POSITION_BOTTOM_RIGHT, RoundedCornerCompat.POSITION_BOTTOM_LEFT)
    val roundedCorners = positions.map { insets.getRoundedCorner(it) }
    if (roundedCorners.all { it == null }) return null
    val values = roundedCorners.map { it?.radius?.toFloat() ?: 0f }
    return DeviceCorners(values[0], values[1], values[2], values[3])
  }
  var corners by remember(view) { mutableStateOf(readCorners()) }
  DisposableEffect(view) {
    // Observe without replacing Compose's insets listener. Covers attach, rotation and window changes.
    val observer = view.viewTreeObserver
    val listener = ViewTreeObserver.OnPreDrawListener {
      corners = readCorners()
      true
    }
    observer.addOnPreDrawListener(listener)
    onDispose {
      val currentObserver = if (observer.isAlive) observer else view.viewTreeObserver
      if (currentObserver.isAlive) currentObserver.removeOnPreDrawListener(listener)
    }
  }
  return corners
}
