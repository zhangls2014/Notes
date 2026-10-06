package me.zhangls.entry

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Window corner radii in pixels, independent of layout direction. */
internal data class DeviceCorners(
  val topLeft: Float,
  val topRight: Float,
  val bottomRight: Float,
  val bottomLeft: Float,
)

@Composable
internal expect fun rememberDeviceCorners(): DeviceCorners?

/** Shared design fallback: 48pt on iOS, represented as 48dp in Compose. */
internal fun DeviceCorners?.orDefault(density: Density): DeviceCorners {
  if (this != null) return this
  val radius = with(density) { 48.dp.toPx() }
  return DeviceCorners(radius, radius, radius, radius)
}

/** Fixed device radii while dragging; visibility only changes during cancellation restoration. */
internal class DeviceCornerShape(
  private val corners: DeviceCorners,
  private val scaleX: Float = 1f,
  private val scaleY: Float = 1f,
  private val cornerVisibility: Float = 1f,
) : Shape {
  override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
    val rect = Rect(0f, 0f, size.width, size.height)
    val visibility = cornerVisibility.coerceIn(0f, 1f)
    fun radius(value: Float): CornerRadius {
      val radiusX = (value * visibility / scaleX.coerceAtLeast(0.001f)).coerceIn(0f, rect.width / 2f)
      val radiusY = (value * visibility / scaleY.coerceAtLeast(0.001f)).coerceIn(0f, rect.height / 2f)
      return CornerRadius(radiusX, radiusY)
    }
    return Outline.Rounded(RoundRect(
      left = rect.left, top = rect.top, right = rect.right, bottom = rect.bottom,
      topLeftCornerRadius = radius(corners.topLeft),
      topRightCornerRadius = radius(corners.topRight),
      bottomRightCornerRadius = radius(corners.bottomRight),
      bottomLeftCornerRadius = radius(corners.bottomLeft),
    ))
  }
}
