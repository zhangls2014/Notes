package me.zhangls.email.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.BottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal expect fun DraftModalBottomSheet(
  viewport: DpSize,
  sheetState: SheetState,
  onDismissRequest: () -> Unit,
  contentWindowInsets: @Composable () -> WindowInsets,
  content: @Composable ColumnScope.() -> Unit,
)

/** The iOS dialog's preferred-size pass must measure the full viewport, including the scrim. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DraftSheetLayout(
  viewport: DpSize,
  sheetState: SheetState,
  onDismissRequest: () -> Unit,
  onScrimClick: () -> Unit,
  contentWindowInsets: @Composable () -> WindowInsets,
  content: @Composable ColumnScope.() -> Unit,
) {
  val scrimAlpha by animateFloatAsState(
    targetValue = if (sheetState.targetValue != SheetValue.Hidden) 1f else 0f,
  )
  Box(Modifier.requiredSize(viewport.width, viewport.height)) {
    Box(
      Modifier.fillMaxSize()
        .background(BottomSheetDefaults.ScrimColor.copy(alpha = BottomSheetDefaults.ScrimColor.alpha * scrimAlpha))
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onScrimClick,
        ),
    )
    BottomSheet(
      modifier = Modifier.align(Alignment.TopCenter),
      state = sheetState,
      onDismissRequest = onDismissRequest,
      contentWindowInsets = contentWindowInsets,
      content = content,
    )
  }
  if (sheetState.hasExpandedState) {
    LaunchedEffect(sheetState) { sheetState.show() }
  }
}
