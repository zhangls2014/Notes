package me.zhangls.login

import kotlin.test.Test
import kotlin.test.assertEquals

class LoginFormLayoutTest {
  @Test
  fun bodyIsCenteredWhenViewportIsTallEnough() {
    assertEquals(
      LoginFormPlacement(height = 800, bodyTop = 190),
      loginFormPlacement(viewportHeight = 800, settingsHeight = 56, bodyHeight = 420),
    )
  }

  @Test
  fun bodyStartsBelowSettingsAndCanScrollWhenViewportIsShort() {
    assertEquals(
      LoginFormPlacement(height = 476, bodyTop = 56),
      loginFormPlacement(viewportHeight = 400, settingsHeight = 56, bodyHeight = 420),
    )
  }

  @Test
  fun centeredBodyNeverOverlapsSettings() {
    assertEquals(
      LoginFormPlacement(height = 500, bodyTop = 80),
      loginFormPlacement(viewportHeight = 500, settingsHeight = 80, bodyHeight = 400),
    )
  }
}
