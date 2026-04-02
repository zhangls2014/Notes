package me.zhangls.entry.util

import android.content.Context
import android.content.pm.PackageInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual object AppInfo : KoinComponent {
  private val context: Context by inject()

  private fun packageInfo(): PackageInfo {
    return with(context) {
      packageManager.getPackageInfo(packageName, 0)
    }
  }

  actual fun getVersionCode() = packageInfo().longVersionCode

  actual fun getVersionName() = packageInfo().versionName ?: ""
}
