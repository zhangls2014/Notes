package me.zhangls.theme.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable

@Composable
actual fun rememberImeHandoffInsets(focusState: ImeHandoffFocusState): WindowInsets = WindowInsets.ime
