package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.MutableWindowInsets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlin.math.roundToInt

/** Consume space outside this pane without padding or moving the pane itself. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PaneWindowInsets(content: @Composable () -> Unit) {
  val outsidePane = remember { MutableWindowInsets() }
  Box(
    modifier = Modifier
      .fillMaxSize()
      .onGloballyPositioned { coordinates ->
        val rootSize = coordinates.findRootCoordinates().size
        val position = coordinates.positionInRoot()
        outsidePane.insets = WindowInsets(
          left = position.x.roundToInt().coerceAtLeast(0),
          top = position.y.roundToInt().coerceAtLeast(0),
          right = (rootSize.width - position.x - coordinates.size.width).roundToInt().coerceAtLeast(0),
          bottom = (rootSize.height - position.y - coordinates.size.height).roundToInt().coerceAtLeast(0),
        )
      }
      .consumeWindowInsets(outsidePane),
    content = { content() },
  )
}
