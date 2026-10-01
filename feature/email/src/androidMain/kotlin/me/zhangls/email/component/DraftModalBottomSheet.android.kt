package me.zhangls.email.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal actual fun DraftModalBottomSheet(
  viewport: DpSize,
  sheetState: SheetState,
  onDismissRequest: () -> Unit,
  contentWindowInsets: @Composable () -> WindowInsets,
  content: @Composable ColumnScope.() -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    contentWindowInsets = contentWindowInsets,
    content = content,
  )
}
