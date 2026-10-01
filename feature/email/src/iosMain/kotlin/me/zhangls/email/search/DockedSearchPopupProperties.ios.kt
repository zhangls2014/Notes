package me.zhangls.email.search

import androidx.compose.ui.window.PopupProperties

internal actual fun dockedSearchPopupProperties(): PopupProperties = PopupProperties(
  focusable = true,
  clippingEnabled = false,
  // Material 的位置提供器返回 positionInWindow；iOS Popup 默认再加安全区原点，
  // 横屏时会重复叠加左侧 inset。锚点本身已由页面处理安全区，这里保持窗口坐标。
  usePlatformInsets = false,
)
