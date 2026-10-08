package me.zhangls.theme.layout

import androidx.compose.ui.Modifier

/** Links a scrolling container to Android IME gestures; other platforms keep their scrolling. */
expect fun Modifier.imeGestureControl(): Modifier
