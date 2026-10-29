package me.zhangls.about

import me.zhangls.about.api.OpenSourcePage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NoticesNavigationTest {
  private val libraries = "file:///app/resources/android.html"

  @Test fun individualDetailsKeepIdentityWithoutFragment() {
    for (id in listOf("license-MIT", "license-Apache-2.0", "license-BSD-3-Clause", "notice-809fa1ed21450f59")) {
      val uri = "file:///app/resources/android-$id.html"
      assertEquals(uri, noticesPageUri(libraries, OpenSourcePage.LICENSES, id))
      assertEquals(
        LocalNoticesLink(OpenSourcePage.LICENSES, id),
        localNoticesLink("$uri?fontScale=2&theme=dark&lang=zh-CN#ignored", libraries),
      )
    }
    assertEquals(LocalNoticesLink(OpenSourcePage.LIBRARIES, null), localNoticesLink(libraries, libraries))
    assertEquals(libraries, noticesPageUri(libraries, OpenSourcePage.LIBRARIES, null))
  }

  @Test fun unbundledOrRemotePagesAreNotInternalDestinations() {
    val unbundled = listOf(
      "file:///etc/passwd",
      "$libraries.evil",
      "https://example.com/android-license-MIT.html",
      "file:///app/resources/ios-license-MIT.html",
      "file:///app/resources/android-licenses.html#license-MIT",
      "file:///app/resources/android-license-../secret.html",
      "file:///app/resources/android-license-%2e%2e.html",
      "file:///app/resources/android-license-..html",
      "file:///app/resources/android-notice-wrong.html",
      "file:///app/resources/android-notice-809fa1ed21450f59.html/extra",
      "javascript:alert(1)",
    )
    for (url in unbundled) assertNull(localNoticesLink(url, libraries))
  }

  @Test fun invalidDetailsCannotBecomeResourcePaths() {
    for (id in listOf(null, "", "licenses", "license-../secret", "notice-invalid")) {
      assertFailsWith<IllegalArgumentException> { noticesPageUri(libraries, OpenSourcePage.LICENSES, id) }
    }
  }
}
