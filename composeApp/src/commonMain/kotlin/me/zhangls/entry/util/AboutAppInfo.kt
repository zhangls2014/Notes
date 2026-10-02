package me.zhangls.entry.util

import me.zhangls.about.api.AboutAppInfo

/** Package metadata is read by the host, never by the feature UI. */
internal fun AppInfo.toAboutAppInfo(): AboutAppInfo = AboutAppInfo(
  versionName = getVersionName().takeIf { it.isNotBlank() },
  buildNumber = getBuildNumber()?.takeIf { it.isNotBlank() },
)
