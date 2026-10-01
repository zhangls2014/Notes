package me.zhangls.entry

import androidx.compose.runtime.Composable

// UIKit does not expose public physical display corner radii; keep the platform default animation.
@Composable
internal actual fun rememberDeviceCorners(): DeviceCorners? = null
