package me.zhangls.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

internal actual fun alertDialogWindowProperties(properties: DialogProperties): DialogProperties = properties

@Composable
internal actual fun alertDialogHorizontalMargin(): Dp = 0.dp
