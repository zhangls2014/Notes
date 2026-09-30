package me.zhangls.login

import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun Modifier.loginImePadding(focusState: LoginInputFocusState): Modifier = imePadding()
