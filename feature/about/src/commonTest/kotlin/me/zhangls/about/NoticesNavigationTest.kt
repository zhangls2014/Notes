package me.zhangls.about

import me.zhangls.about.api.OpenSourcePage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NoticesNavigationTest {
  private val libraries = "file:///app/resources/android.html"
  private val licenses = "file:///app/resources/android-licenses.html"

  @Test fun localPagesKeepDestinationAndAnchor() {
    assertEquals(
      LocalNoticesLink(OpenSourcePage.LICENSES, "license-MIT"),
      localNoticesLink("$licenses?fontScale=2&theme=dark#license-MIT", libraries, licenses),
    )
    assertEquals(LocalNoticesLink(OpenSourcePage.LIBRARIES, null), localNoticesLink(libraries, libraries, licenses))
  }

  @Test fun unbundledOrRemotePagesAreNotInternalDestinations() {
    val unbundled = listOf(
      "file:///etc/passwd",
      "$libraries.evil",
      "https://example.com/android.html",
      "javascript:alert(1)",
    )
    for (url in unbundled) {
      assertNull(localNoticesLink(url, libraries, licenses))
    }
  }
}
