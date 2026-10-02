package me.zhangls.framework.toast

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToastLayoutTest {
  @Test
  fun centersToastWithinTheHostWindowInsteadOfTheScreen() {
    val frame = toastFrame(400.0, 800.0, ToastInsets(), 120.0, 20.0)
    assertEquals(120.0, frame.x)
    assertEquals(652.0, frame.y)
    assertEquals(160.0, frame.width)
    assertEquals(48.0, frame.height)
  }

  @Test
  fun accountsForAsymmetricSafeAreaAndBottomObstruction() {
    val insets = ToastInsets(left = 80.0, right = 20.0, bottom = 150.0)
    val frame = toastFrame(500.0, 800.0, insets, 120.0, 20.0)
    assertEquals(200.0, frame.x)
    assertEquals(570.0, frame.y)
  }

  @Test
  fun clampsLongMessagesToSmallWindowSafeArea() {
    val frame = toastFrame(
      180.0, 200.0, ToastInsets(top = 80.0, bottom = 100.0), 500.0, 500.0,
    )
    assertEquals(30.0, frame.x)
    assertEquals(80.0, frame.y)
    assertEquals(120.0, frame.width)
    assertEquals(20.0, frame.height)
    assertTrue(frame.y + frame.height <= 100.0)
  }

  @Test
  fun recalculatesPositionWhenTheWindowResizes() {
    val portrait = toastFrame(400.0, 800.0, ToastInsets(), 120.0, 20.0)
    val landscape = toastFrame(800.0, 400.0, ToastInsets(), 120.0, 20.0)
    assertEquals(portrait.x + 200.0, landscape.x)
    assertEquals(portrait.y - 400.0, landscape.y)
  }
}
