package me.zhangls.entry

import me.zhangls.entry.util.AppInfo
import me.zhangls.entry.util.toAboutAppInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AboutAppInfoTest {
  @Test fun retainsPlatformVersionAndDottedBuildIdentifier() {
    val info = appInfo("2.3.1", "42.7.1").toAboutAppInfo()
    assertEquals("2.3.1", info.versionName)
    assertEquals("42.7.1", info.buildNumber)
  }

  @Test fun unavailableMetadataDoesNotBecomeAnInventedVersion() {
    val info = appInfo("  ", null).toAboutAppInfo()
    assertNull(info.versionName)
    assertNull(info.buildNumber)
    assertNull(appInfo("", " ").toAboutAppInfo().buildNumber)
  }

  private fun appInfo(version: String, build: String?) = object : AppInfo {
    override fun getVersionName() = version
    override fun getVersionCode() = 0L
    override fun getBuildNumber() = build
  }
}
