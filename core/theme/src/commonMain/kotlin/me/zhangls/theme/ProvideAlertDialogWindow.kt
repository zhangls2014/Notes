package me.zhangls.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.BasicAlertDialogOverride
import androidx.compose.material3.BasicAlertDialogOverrideScope
import androidx.compose.material3.ExperimentalMaterial3ComponentOverrideApi
import androidx.compose.material3.LocalBasicAlertDialogOverride
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import notes.core.theme.generated.resources.Res
import notes.core.theme.generated.resources.theme_dialog_pane_title
import org.jetbrains.compose.resources.stringResource

/** Sizes alerts against the application window, retaining edge margins rather than preferred widths. */
@OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
@Composable
internal fun ProvideAlertDialogWindow(content: @Composable () -> Unit) {
  // Read outside Dialog: its own window must not become the reference for the next measurement.
  val density = LocalDensity.current
  val insets = WindowInsets.safeDrawing
  val layoutDirection = LocalLayoutDirection.current
  val safeMargin = with(density) {
    maxOf(insets.getLeft(density, layoutDirection), insets.getRight(density, layoutDirection)).toDp()
  }
  val margin = alertDialogHorizontalMargin() + safeMargin
  val windowSize = LocalWindowInfo.current.containerSize
  val maxWidth = with(density) { alertDialogWidth(windowSize.width.toDp(), margin) }
  val maxHeight = with(density) {
    val verticalMargin = maxOf(insets.getTop(density), insets.getBottom(density))
    (windowSize.height - verticalMargin * 2).coerceAtLeast(0).toDp()
  }
  val override = remember(maxWidth, maxHeight) { WindowAlertDialogOverride(maxWidth, maxHeight) }
  CompositionLocalProvider(LocalBasicAlertDialogOverride provides override, content = content)
}

@OptIn(ExperimentalMaterial3ComponentOverrideApi::class)
private class WindowAlertDialogOverride(
  private val maxWidth: Dp,
  private val maxHeight: Dp,
) : BasicAlertDialogOverride {
  @Composable
  override fun BasicAlertDialogOverrideScope.BasicAlertDialog() {
    Dialog(onDismissRequest = onDismissRequest, properties = alertDialogWindowProperties(properties)) {
      val paneDescription = stringResource(Res.string.theme_dialog_pane_title)
      // Keep Material's content and minimum width, replacing only its container's maximum size.
      Box(
        modifier = modifier
          .sizeIn(minWidth = min(ALERT_DIALOG_MIN_WIDTH.dp, maxWidth), maxWidth = maxWidth, maxHeight = maxHeight)
          .semantics { paneTitle = paneDescription },
        propagateMinConstraints = true,
      ) {
        content()
      }
    }
  }
}

internal expect fun alertDialogWindowProperties(properties: DialogProperties): DialogProperties

@Composable
internal expect fun alertDialogHorizontalMargin(): Dp

internal fun alertDialogWidth(windowWidth: Dp, horizontalMargin: Dp): Dp =
  min(windowWidth * ALERT_DIALOG_WIDTH_FRACTION, (windowWidth - horizontalMargin * 2).coerceAtLeast(0.dp))

private const val ALERT_DIALOG_WIDTH_FRACTION = 0.8f

private const val ALERT_DIALOG_MIN_WIDTH = 280
