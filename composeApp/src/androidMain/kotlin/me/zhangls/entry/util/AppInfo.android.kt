package me.zhangls.entry.util

import android.content.Context
import android.content.pm.PackageInfo
import org.koin.core.annotation.Singleton

@Singleton(binds = [AppInfo::class])
class AndroidAppInfo(context: Context) : AppInfo {
  private val applicationContext = context.applicationContext

  private fun packageInfo(): PackageInfo {
    return with(applicationContext) {
      packageManager.getPackageInfo(packageName, 0)
    }
  }

  override fun getVersionCode() = packageInfo().longVersionCode

  override fun getVersionName() = packageInfo().versionName ?: ""
}
