package me.zhangls.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeColor {
  Error,
  Primary,
  Secondary,
}

@Composable
fun ThemeColor.toColor(): Color {
  val scheme = MaterialTheme.colorScheme
  return when (this) {
    ThemeColor.Error -> scheme.error
    ThemeColor.Primary -> scheme.primary
    ThemeColor.Secondary -> scheme.secondary
  }
}
