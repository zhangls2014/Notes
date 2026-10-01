package me.zhangls.email.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
internal actual fun DraftModalBottomSheet(
  viewport: DpSize,
  sheetState: SheetState,
  onDismissRequest: () -> Unit,
  contentWindowInsets: @Composable () -> WindowInsets,
  content: @Composable ColumnScope.() -> Unit,
) {
  val scope = rememberCoroutineScope()
  val dismiss: () -> Unit = {
    scope.launch {
      sheetState.hide()
      if (!sheetState.isVisible) onDismissRequest()
    }
  }
  key(viewport) {
    Dialog(
      onDismissRequest = dismiss,
      properties = DialogProperties(
        usePlatformDefaultWidth = false,
        usePlatformInsets = false,
        useSoftwareKeyboardInset = false,
        scrimColor = Color.Transparent,
        animateTransition = false,
      ),
    ) {
      DraftSheetLayout(
        viewport = viewport,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        onScrimClick = dismiss,
        contentWindowInsets = contentWindowInsets,
        content = content,
      )
    }
  }
}
