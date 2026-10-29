package me.zhangls.theme

import androidx.compose.runtime.Composable

@Composable
internal actual fun ProvideAlertDialogWindow(content: @Composable () -> Unit) {
  content()
}
