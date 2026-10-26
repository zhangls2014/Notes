package me.zhangls.theme.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable

/** IME insets with temporary Android protection for handoffs between form fields. */
@Composable
expect fun rememberImeHandoffInsets(focusState: ImeHandoffFocusState): WindowInsets
