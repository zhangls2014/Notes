package me.zhangls.email.search

import androidx.compose.ui.window.PopupProperties

internal actual fun dockedSearchPopupProperties(): PopupProperties = PopupProperties(
  focusable = true,
  clippingEnabled = false,
)
