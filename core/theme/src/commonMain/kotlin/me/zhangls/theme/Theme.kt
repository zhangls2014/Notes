package me.zhangls.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density


@Composable
fun ComposeAppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicScheme: ColorScheme? = null,
  fontScale: Float = 1F,
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicScheme != null -> dynamicScheme
    darkTheme -> darkScheme
    else -> lightScheme
  }

  val density = LocalDensity.current

  CompositionLocalProvider(
    LocalDensity provides Density(density = density.density, fontScale = fontScale),
  ) {
    MaterialTheme(colorScheme = colorScheme, content = content)
  }
}
