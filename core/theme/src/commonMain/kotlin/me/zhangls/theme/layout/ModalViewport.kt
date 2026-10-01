package me.zhangls.theme.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize

/** Full application viewport before navigation or list-detail panes consume space. */
val LocalModalViewport = staticCompositionLocalOf<DpSize> {
  error("Modal viewport must be provided by the application navigation root")
}

/** Supplies finite modal bounds without reading another set of platform window facts. */
@Composable
fun ProvideModalViewport(content: @Composable () -> Unit) {
  BoxWithConstraints(Modifier.fillMaxSize()) {
    CompositionLocalProvider(LocalModalViewport provides DpSize(maxWidth, maxHeight)) {
      content()
    }
  }
}
