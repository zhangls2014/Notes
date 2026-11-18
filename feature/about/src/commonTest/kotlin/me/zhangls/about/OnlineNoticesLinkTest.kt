package me.zhangls.about

import kotlin.test.Test
import kotlin.test.assertEquals

class OnlineNoticesLinkTest {
  @Test
  fun websiteAllowsNavigationWithinItsConfiguredHttpsOrigin() {
    val home = "https://website.example.test/"
    val downloads = "${home}download/"
    val notices = "${home}open-source/$releasePlatform-licenses.html#license-MIT"
    val external = "https://downloads.example.test/notes.apk"
    val otherPort = "https://website.example.test:8443/"
    val insecure = "http://website.example.test/"

    assertEquals(NoticesLink.InPage, noticesLink(downloads, home, allowSameOriginNavigation = true))
    assertEquals(NoticesLink.InPage, noticesLink(notices, home, allowSameOriginNavigation = true))
    assertEquals(NoticesLink.Project(external), noticesLink(external, home, allowSameOriginNavigation = true))
    assertEquals(NoticesLink.Project(otherPort), noticesLink(otherPort, home, allowSameOriginNavigation = true))
    assertEquals(NoticesLink.Project(insecure), noticesLink(insecure, home, allowSameOriginNavigation = true))
    assertEquals(NoticesLink.Blocked, noticesLink("file:///etc/hosts", home, allowSameOriginNavigation = true))
  }

  @Test
  fun onlyPlatformNoticesOnTheConfiguredHttpsOriginCanLoadInTheView() {
    val origin = "https://website.example.test"
    val list = "$origin/open-source/$releasePlatform.html"
    val detail = "/open-source/$releasePlatform-licenses.html"

    assertEquals(NoticesLink.InPage, noticesLink("$list?lang=zh", list))
    assertEquals(NoticesLink.Detail("license-Apache-2.0"), noticesLink("$origin$detail#license-Apache-2.0", list))
    assertEquals(NoticesLink.Blocked, noticesLink("$origin$detail#license-Apache..2.0", list))
    assertEquals(NoticesLink.Detail("license-MIT"), noticesLink("$origin$detail#license-MIT", list))
    assertEquals(NoticesLink.Blocked, noticesLink("$origin$detail#../license-MIT", list))
    assertEquals(NoticesLink.Blocked, noticesLink("$origin$detail", list))
    val selected = "$origin$detail#license-MIT"
    assertEquals(NoticesLink.InPage, noticesLink(selected, selected))
    assertEquals(NoticesLink.Detail("notice-abc"), noticesLink("$origin$detail#notice-abc", selected))
    assertEquals(NoticesLink.Project("https://outside.example.test$detail"), noticesLink("https://outside.example.test$detail", list))
    assertEquals(NoticesLink.Project("$origin/account"), noticesLink("$origin/account", list))
    assertEquals(NoticesLink.Project("$origin:8443$detail"), noticesLink("$origin:8443$detail", list))
    assertEquals(NoticesLink.Blocked, noticesLink("https://website.example.test@outside.example.test/", list))
    assertEquals(NoticesLink.Project("http://website.example.test$detail"), noticesLink("http://website.example.test$detail", list))
    assertEquals(NoticesLink.InPage, noticesLink("https://outside.example.test/", list, projectPage = true))
    assertEquals(NoticesLink.Blocked, noticesLink("intent://outside.example.test", list, projectPage = true))
    assertEquals(NoticesLink.Blocked, noticesLink("file:///etc/hosts", list))
  }
}
