package me.zhangls.entry

import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.api.EmailNavigation
import me.zhangls.framework.deeplink.DeepLinkMatcher
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.MainNavigation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComposeAppCommonTest {
  private val siteOrigin = "https://site.example.test"

  /** 与组合根同源的匹配器集合：路径表由各 feature 的 `-api` 提供。 */
  private val matchers = listOf(MainNavigation, EmailNavigation).flatMap { it.deepLinkMatchers }

  @Test
  fun `parse email deep link success`() {
    val destination = parseDeepLink("https://site.example.test/email?id=1", matchers, siteOrigin)
    assertEquals(EmailDetailDestination(1), destination)
  }

  @Test
  fun `parse home deep link success`() {
    val destination = parseDeepLink("https://site.example.test/home", matchers, siteOrigin)
    assertEquals(HomeDestination, destination)
  }

  @Test
  fun `parse deep link fail when id missing`() {
    val destination = parseDeepLink("https://site.example.test/email", matchers, siteOrigin)
    assertNull(destination)
  }

  @Test
  fun `parse deep link fail when id is invalid`() {
    val destination = parseDeepLink("https://site.example.test/email?id=abc", matchers, siteOrigin)
    assertNull(destination)
  }

  @Test
  fun `parse deep link fail when host not matched`() {
    val destination = parseDeepLink("https://example.com/email?id=1", matchers, siteOrigin)
    assertNull(destination)
  }

  @Test
  fun `parse deep link fail when path not matched`() {
    val destination = parseDeepLink("https://site.example.test/unknown?id=1", matchers, siteOrigin)
    assertNull(destination)
  }

  @Test
  fun `reject invalid links before invoking matchers`() {
    val recordingMatcher = DeepLinkMatcher { error("Invalid links must not reach matchers") }
    listOf(
      null, "", "http://site.example.test/home", "https://example.com/home",
      "https://site.example.test:444/home", "https://other@site.example.test/home",
    ).forEach {
      assertNull(parseDeepLink(it, listOf(recordingMatcher), siteOrigin))
    }
  }

  @Test
  fun `first matching destination wins`() {
    var laterMatcherCalled = false
    val first = DeepLinkMatcher { HomeDestination }
    val later = DeepLinkMatcher {
      laterMatcherCalled = true
      EmailDetailDestination(1)
    }
    assertEquals(HomeDestination, parseDeepLink("https://site.example.test/home", listOf(first, later), siteOrigin))
    assertTrue(!laterMatcherCalled)
  }
}
