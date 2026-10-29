package me.zhangls.entry

import me.zhangls.about.api.AboutAppInfo
import me.zhangls.framework.AppInfo
import org.koin.mp.KoinPlatform

/** Deterministic installed-package source for about UI tests. */
internal fun provideAboutAppInfo(info: AboutAppInfo = AboutAppInfo("1.0", "1")) {
  KoinPlatform.getKoin().declare<AppInfo>(
    object : AppInfo {
      override fun getVersionCode() = 0L
      override fun getVersionName() = info.versionName.orEmpty()
      override fun getBuildNumber() = info.buildNumber
    },
  )
}
