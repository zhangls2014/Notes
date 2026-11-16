package me.zhangls.theme.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun rememberImeHandoffInsets(focusState: ImeHandoffFocusState): WindowInsets = WindowInsets(0, 0, 0, 0)

actual fun Modifier.imeGestureControl(): Modifier = this
