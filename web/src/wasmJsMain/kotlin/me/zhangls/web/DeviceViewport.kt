@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package me.zhangls.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class DeviceViewportSnapshot(
  val features: List<ViewportFeature> = emptyList(),
  val devicePlatform: String? = null,
)

@Serializable
internal data class ViewportFeature(
  val bounds: ViewportBounds,
  val orientation: String,
  val state: String,
  val separating: Boolean,
  val occlusion: String,
)

@Serializable
internal data class ViewportBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
  fun rect(density: Float): Rect = Rect(left * density, top * density, right * density, bottom * density)
}

private val viewportJson = Json { ignoreUnknownKeys = true }

@Composable
internal fun rememberDeviceViewport(): DeviceViewportSnapshot {
  var snapshot by remember { mutableStateOf(DeviceViewportSnapshot()) }
  DisposableEffect(Unit) {
    val unsubscribe = subscribeDeviceViewport { json ->
      snapshot = viewportJson.decodeFromString<DeviceViewportSnapshot>(json).copy(devicePlatform = devicePlatform())
    }
    onDispose { unsubscribe() }
  }
  return snapshot
}

private fun subscribeDeviceViewport(callback: (String) -> Unit): () -> Unit = js("""{
  if (!window.deviceViewport) return () => {};
  return window.deviceViewport.subscribe(value => callback(JSON.stringify(value)));
}""")

internal fun devicePlatform(): String? = js("""{
  const host = document.documentElement.dataset.hostPlatform;
  if (host === 'android' || host === 'ios') return host;
  if (/iPhone|iPad|iPod/.test(navigator.userAgent) ||
      (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)) return 'ios';
  return /Android/i.test(navigator.userAgent) ? 'android' : null;
}""")
