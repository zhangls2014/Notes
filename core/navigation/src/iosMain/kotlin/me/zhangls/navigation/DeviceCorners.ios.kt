package me.zhangls.navigation

import androidx.compose.runtime.Composable

// Native window corner reading is not implemented; navigation applies the shared default radius.
@Composable
internal actual fun rememberDeviceCorners(): DeviceCorners? = null
