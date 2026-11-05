package me.zhangls.theme

import android.graphics.Rect
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties

internal actual fun alertDialogWindowProperties(properties: DialogProperties): DialogProperties = DialogProperties(
  dismissOnBackPress = properties.dismissOnBackPress,
  dismissOnClickOutside = properties.dismissOnClickOutside,
  securePolicy = properties.securePolicy,
  usePlatformDefaultWidth = false,
  decorFitsSystemWindows = false,
  windowTitle = properties.windowTitle,
  windowType = properties.windowType,
  windowToken = properties.windowToken,
)

@Composable
internal actual fun alertDialogHorizontalMargin(): Dp {
  val context = LocalContext.current
  val configuration = LocalConfiguration.current
  val padding = remember(context, configuration) {
    val dialogTheme = TypedValue()
    context.theme.resolveAttribute(android.R.attr.dialogTheme, dialogTheme, true)
    val dialogContext = ContextThemeWrapper(context, dialogTheme.resourceId)
    val attributes = dialogContext.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
    try {
      val bounds = Rect()
      attributes.getDrawable(0)?.getPadding(bounds)
      maxOf(bounds.left, bounds.right)
    } finally {
      attributes.recycle()
    }
  }
  return with(LocalDensity.current) { padding.toDp() }
}
