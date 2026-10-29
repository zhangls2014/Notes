package me.zhangls.about

import me.zhangls.about.api.AboutAppInfo
import me.zhangls.framework.AppInfo

/** Convert the injected platform metadata to the about presentation model. */
internal fun AppInfo.toAboutAppInfo(): AboutAppInfo = AboutAppInfo(
  versionName = getVersionName().takeIf { it.isNotBlank() },
  buildNumber = getBuildNumber()?.takeIf { it.isNotBlank() },
)
