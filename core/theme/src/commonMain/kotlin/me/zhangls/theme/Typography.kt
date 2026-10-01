package me.zhangls.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/** Emphasize headings and actions while preserving the body scale for reading. */
internal val AppTypography = Typography(fontFamily = FontFamily.Default).let { base ->
  // The compatibility no-argument constructor aliases emphasized styles to regular ones.
  base.copy(
    displayLarge = base.displayLargeEmphasized,
    displayMedium = base.displayMediumEmphasized,
    displaySmall = base.displaySmallEmphasized,
    headlineLarge = base.headlineLargeEmphasized,
    headlineMedium = base.headlineMediumEmphasized,
    headlineSmall = base.headlineSmallEmphasized,
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMediumEmphasized,
    titleSmall = base.titleSmallEmphasized,
    labelLarge = base.labelLargeEmphasized,
    labelMedium = base.labelMediumEmphasized,
    labelSmall = base.labelSmallEmphasized,
  )
}
