package me.zhangls.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density


@Composable
fun ComposeAppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicScheme: ColorScheme? = null,
  // 应用字号是相对系统设置的倍率，标准字号始终跟随系统。
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
    LocalDensity provides if (fontScale == 1F) density else Density(
      density = density.density,
      fontScale = density.fontScale * fontScale,
    ),
  ) {
    MaterialExpressiveTheme(
      colorScheme = colorScheme,
      motionScheme = MotionScheme.expressive(),
      typography = AppTypography,
      shapes = AppShapes,
      content = content,
    )
  }
}
