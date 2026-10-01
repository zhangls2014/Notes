package me.zhangls.email.search

import androidx.compose.ui.window.PopupProperties

/** 停靠搜索弹层与 Material 搜索锚点使用同一窗口坐标系。 */
internal expect fun dockedSearchPopupProperties(): PopupProperties
