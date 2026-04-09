package me.zhangls.entry

import me.zhangls.email.detail.EmailDetailDestination
import me.zhangls.main.MainDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ComposeAppCommonTest {

  @Test
  fun `parse email deep link success`() {
    val destination = parseDeepLink("https://notes.zhangls.me/email?id=1")
    assertEquals(EmailDetailDestination(1), destination)
  }

  @Test
  fun `parse home deep link success`() {
    val destination = parseDeepLink("https://notes.zhangls.me/home")
    assertEquals(MainDestination, destination)
  }

  @Test
  fun `parse deep link fail when id missing`() {
    val destination = parseDeepLink("https://notes.zhangls.me/email")
    assertNull(destination)
  }

  @Test
  fun `parse deep link fail when id is invalid`() {
    val destination = parseDeepLink("https://notes.zhangls.me/email?id=abc")
    assertNull(destination)
  }

  @Test
  fun `parse deep link fail when host not matched`() {
    val destination = parseDeepLink("https://example.com/email?id=1")
    assertNull(destination)
  }

  @Test
  fun `parse deep link fail when path not matched`() {
    val destination = parseDeepLink("https://notes.zhangls.me/unknown?id=1")
    assertNull(destination)
  }
}
