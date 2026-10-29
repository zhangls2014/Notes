package me.zhangls.theme

import androidx.compose.runtime.Composable

/** Keeps Android alert dialogs centered in the full window, independently of system bar visibility. */
@Composable
internal expect fun ProvideAlertDialogWindow(content: @Composable () -> Unit)
