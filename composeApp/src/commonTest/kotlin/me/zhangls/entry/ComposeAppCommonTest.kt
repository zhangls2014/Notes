package me.zhangls.entry

import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.api.EmailNavigation
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.MainNavigation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ComposeAppCommonTest {

  /** 与组合根同源的匹配器集合：路径表由各 feature 的 `-api` 提供。 */
  private val matchers = listOf(MainNavigation, EmailNavigation).flatMap { it.deepLinkMatchers }

  @Test
  fun `parse email deep link success`() {
    val destination = parseDeepLink("https://notes.zhangls.me/email?id=1", matchers)
    assertEquals(EmailDetailDestination(1), destination)
  }

  @Test
  fun `parse home deep link success`() {
    val destination = parseDeepLink("https://notes.zhangls.me/home", matchers)
    assertEquals(HomeDestination, destination)
  }

  @Test
  fun `parse deep link fail when id missing`() {
    val destination = parseDeepLink("https://notes.zhangls.me/email", matchers)
    assertNull(destination)
  }

  @Test
  fun `parse deep link fail when id is invalid`() {
    val destination = parseDeepLink("https://notes.zhangls.me/email?id=abc", matchers)
    assertNull(destination)
  }

  @Test
  fun `parse deep link fail when host not matched`() {
    val destination = parseDeepLink("https://example.com/email?id=1", matchers)
    assertNull(destination)
  }

  @Test
  fun `parse deep link fail when path not matched`() {
    val destination = parseDeepLink("https://notes.zhangls.me/unknown?id=1", matchers)
    assertNull(destination)
  }
}
