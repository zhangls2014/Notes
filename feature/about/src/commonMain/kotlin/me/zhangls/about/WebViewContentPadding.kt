package me.zhangls.about

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection

/** Keep the viewport behind the gesture area; its inset is applied inside the scroller. */
internal fun PaddingValues.webViewContentPadding(direction: LayoutDirection): PaddingValues = PaddingValues(
  start = calculateStartPadding(direction),
  top = calculateTopPadding(),
  end = calculateEndPadding(direction),
)
