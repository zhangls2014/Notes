package me.zhangls.entry

import android.content.Context
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.framework.AppInfo
import org.koin.mp.KoinPlatform
import org.robolectric.RuntimeEnvironment

/** Deterministic installed-package source for about UI tests. */
internal fun provideAboutAppInfo(info: AboutAppInfo = AboutAppInfo("1.0", "1"), channel: String? = null) {
  KoinPlatform.getKoin().declare<Context>(RuntimeEnvironment.getApplication())
  KoinPlatform.getKoin().declare<AppInfo>(testAppInfo(info, channel))
}

internal fun testAppInfo(info: AboutAppInfo = AboutAppInfo("1.0", "1"), channel: String? = null): AppInfo = object : AppInfo {
  override fun getVersionCode() = 0L
  override fun getVersionName() = info.versionName.orEmpty()
  override fun getBuildNumber() = info.buildNumber
  override fun getUpdateChannel() = channel
}
