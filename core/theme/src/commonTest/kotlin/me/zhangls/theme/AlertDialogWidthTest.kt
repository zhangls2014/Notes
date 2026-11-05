package me.zhangls.theme

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class AlertDialogWidthTest {
  @Test fun wideWindowUsesEightyPercent() {
    assertEquals(960.dp, alertDialogWidth(1200.dp, 32.dp))
  }

  @Test fun narrowWindowRetainsMinimumMargins() {
    assertEquals(176.dp, alertDialogWidth(240.dp, 32.dp))
  }

  @Test fun safeAreaAndThemeMarginCanOverridePercentage() {
    assertEquals(200.dp, alertDialogWidth(400.dp, 100.dp))
  }

  @Test fun smallerThanMarginsDoesNotProduceNegativeWidth() {
    assertEquals(0.dp, alertDialogWidth(40.dp, 32.dp))
  }
}
