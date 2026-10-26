package me.zhangls.framework.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkParserTest {
  @Test
  fun rejectsMissingSchemeOrHost() {
    listOf(null, "", "   ", "example.com/path", "://example.com", "https://", "https:///path", "https:// /path")
      .forEach { assertNull(parseDeepLinkUrl(it), it) }
  }

  @Test
  fun normalizesSchemeAndHostButPreservesPathAndQueryValues() {
    assertEquals(
      DeepLinkRequest("https", "example.com", "/Mail", mapOf("id" to "AbC")),
      parseDeepLinkUrl("  HTTPS://Example.COM:443/Mail?id=AbC  "),
    )
  }

  @Test
  fun defaultsPathAndPreservesQuerySplittingRules() {
    assertEquals(DeepLinkRequest("https", "example.com", "/", emptyMap()), parseDeepLinkUrl("https://example.com"))
    assertEquals(
      mapOf("value" to "one=two", "empty" to "", "flag" to "", "duplicate" to "last"),
      parseDeepLinkUrl("https://example.com/?value=one=two&empty=&flag&&duplicate=first&duplicate=last")?.queries,
    )
  }
}
