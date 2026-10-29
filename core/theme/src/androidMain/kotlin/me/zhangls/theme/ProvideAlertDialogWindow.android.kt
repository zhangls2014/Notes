package me.zhangls.theme

import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BasicAlertDialogOverride
import androidx.compose.material3.BasicAlertDialogOverrideScope
import androidx.compose.material3.DefaultBasicAlertDialogOverride
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.LocalBasicAlertDialogOverride
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
@Composable
internal actual fun ProvideAlertDialogWindow(content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalBasicAlertDialogOverride provides FullWindowAlertDialogOverride, content = content)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ComponentOverrideApi::class)
private object FullWindowAlertDialogOverride : BasicAlertDialogOverride {
  @Composable
  override fun BasicAlertDialogOverrideScope.BasicAlertDialog() {
    // Delegate layout, width constraints and accessibility semantics to Material.
    CompositionLocalProvider(LocalBasicAlertDialogOverride provides DefaultBasicAlertDialogOverride) {
      BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        properties = DialogProperties(
          dismissOnBackPress = properties.dismissOnBackPress,
          dismissOnClickOutside = properties.dismissOnClickOutside,
          securePolicy = properties.securePolicy,
          usePlatformDefaultWidth = properties.usePlatformDefaultWidth,
          decorFitsSystemWindows = false,
          windowTitle = properties.windowTitle,
          windowType = properties.windowType,
          windowToken = properties.windowToken,
        ),
        content = {
          CompositionLocalProvider(LocalBasicAlertDialogOverride provides FullWindowAlertDialogOverride) {
            content()
          }
        },
      )
    }
  }
}
