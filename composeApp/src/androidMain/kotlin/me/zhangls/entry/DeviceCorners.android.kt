package me.zhangls.entry

import android.os.Build
import android.view.RoundedCorner
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView

@Composable
internal actual fun rememberDeviceCorners(): DeviceCorners? {
  val view = LocalView.current
  fun readCorners(): DeviceCorners? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val insets = view.rootWindowInsets ?: return null
    val positions = listOf(RoundedCorner.POSITION_TOP_LEFT, RoundedCorner.POSITION_TOP_RIGHT,
      RoundedCorner.POSITION_BOTTOM_RIGHT, RoundedCorner.POSITION_BOTTOM_LEFT)
    val values = positions.map { insets.getRoundedCorner(it)?.radius?.toFloat() ?: 0f }
    if (values.all { it == 0f }) return null
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
