package me.zhangls.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun alertDialogWindowProperties(properties: DialogProperties): DialogProperties = DialogProperties(
  dismissOnBackPress = properties.dismissOnBackPress,
  dismissOnClickOutside = properties.dismissOnClickOutside,
  usePlatformDefaultWidth = false,
  usePlatformInsets = properties.usePlatformInsets,
  useSoftwareKeyboardInset = properties.useSoftwareKeyboardInset,
  scrimColor = properties.scrimColor,
  animateTransition = properties.animateTransition,
)

@Composable
internal actual fun alertDialogHorizontalMargin(): Dp = 0.dp
