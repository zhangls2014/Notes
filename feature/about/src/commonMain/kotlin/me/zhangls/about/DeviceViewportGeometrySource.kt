package me.zhangls.about

import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo
import me.zhangls.theme.layout.normalizeViewportFeature
import kotlin.math.abs

private const val AXIS_ALIGNMENT_TOLERANCE = 0.001f

/** Reads root-owned facts; the native frame callback samples the final measured transform. */
internal class DeviceViewportGeometrySource {
  private var coordinates: LayoutCoordinates? = null
  private var measuredWindowSize: IntSize = IntSize.Zero
  var onFactsChanged: (() -> Unit)? = null

  var windowInfo: WindowAdaptiveInfo? = null
  var windowSize: IntSize = IntSize.Zero

  fun onPositioned(layout: LayoutCoordinates) {
    coordinates = layout
    measuredWindowSize = windowSize
  }

  fun snapshot(): DeviceViewportGeometry {
    val layout = coordinates?.takeIf { it.isAttached && it.size.width > 0 && it.size.height > 0 }
      ?.takeIf { measuredWindowSize == windowSize }
    val info = windowInfo
    return if (layout != null && info != null) geometry(layout, info) else DeviceViewportGeometry()
  }

  private fun geometry(layout: LayoutCoordinates, info: WindowAdaptiveInfo): DeviceViewportGeometry {
    val origin = layout.windowToLocal(Offset.Zero)
    val xAxis = layout.windowToLocal(Offset(1f, 0f)) - origin
    val yAxis = layout.windowToLocal(Offset(0f, 1f)) - origin
    val finite = origin.x.isFinite() && origin.y.isFinite()
    val aligned = abs(xAxis.y) <= AXIS_ALIGNMENT_TOLERANCE && abs(yAxis.x) <= AXIS_ALIGNMENT_TOLERANCE
    val positive = xAxis.x > 0 && yAxis.y > 0
    if (!finite || !aligned || !positive) return DeviceViewportGeometry()
    val viewport = Rect(0f, 0f, layout.size.width.toFloat(), layout.size.height.toFloat())
    val features = info.windowPosture.hingeList.mapNotNull { hinge ->
      val local = Rect(
        layout.windowToLocal(hinge.bounds.topLeft),
        layout.windowToLocal(hinge.bounds.bottomRight),
      )
      normalizeViewportFeature(local, viewport)?.let { feature(hinge, it) }
    }
    return DeviceViewportGeometry(
      "available",
      JsonArray(features).toString(),
      "${layout.size}:$origin:$xAxis:$yAxis:$windowSize",
    )
  }
}

private fun feature(hinge: HingeInfo, bounds: Rect) = buildJsonObject {
  put("kind", "unknown")
  put(
    "bounds",
    buildJsonObject {
      put("left", bounds.left)
      put("top", bounds.top)
      put("right", bounds.right)
      put("bottom", bounds.bottom)
    },
  )
  put("orientation", if (hinge.isVertical) "vertical" else "horizontal")
  put("state", if (hinge.isFlat) "flat" else "half-opened")
  put("separating", hinge.isSeparating)
  put("occlusion", if (hinge.isOccluding) "full" else "none")
}

internal data class DeviceViewportGeometry(
  val availability: String = "unavailable",
  val features: String = "[]",
  val key: String = "",
)

@Composable
internal fun rememberDeviceViewportGeometrySource(enabled: Boolean): DeviceViewportGeometrySource? {
  if (!enabled) return null
  val source = remember { DeviceViewportGeometrySource() }
  val info = LocalWindowAdaptiveInfo.current
  val size = LocalWindowInfo.current.containerSize
  SideEffect {
    val changed = source.windowInfo != info || source.windowSize != size
    source.windowInfo = info
    source.windowSize = size
    if (changed) source.onFactsChanged?.invoke()
  }
  return source
}

internal fun Modifier.deviceViewportGeometry(source: DeviceViewportGeometrySource?): Modifier = onGloballyPositioned {
  source?.onPositioned(it)
}
