package me.zhangls.theme.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * @author zhangls
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContainedLoadingIndicator(
  modifier: Modifier = Modifier,
  backgroundColor: Color = Color.Black.copy(alpha = 0.15F),
  loadingSize: Dp = 64.dp,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(backgroundColor)
      .pointerInput(Unit) {
        awaitPointerEventScope {
          while (true) {
            awaitPointerEvent()
          }
        }
      },
    contentAlignment = Alignment.Center
  ) {
    ContainedLoadingIndicator(modifier = Modifier.size(loadingSize))
  }
}