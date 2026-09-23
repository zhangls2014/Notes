package me.zhangls.theme.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.zhangls.theme.layout.ContentWidth
import me.zhangls.theme.layout.HingeSafeContent
import me.zhangls.theme.layout.contentWidth

/** Full-window coordinate space is essential: an ordinary centered dialog can straddle a fold. */
@Composable
fun HingeSafeDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
  Dialog(
    onDismissRequest = onDismissRequest,
    properties = hingeSafeDialogProperties(),
  ) {
    Box(
      Modifier
        .fillMaxSize()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismissRequest,
        ),
    ) {
      HingeSafeContent(Modifier.safeDrawingPadding().imePadding()) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
          // Consume taps inside the surface; outside taps and system back dismiss normally.
          Box(
            Modifier
              .contentWidth(ContentWidth.Form)
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
              ),
          ) {
            content()
          }
        }
      }
    }
  }
}

internal expect fun hingeSafeDialogProperties(): DialogProperties
