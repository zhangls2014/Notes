package me.zhangls.entry.util

import org.koin.core.annotation.Singleton
import platform.Foundation.NSBundle

@Singleton(binds = [AppInfo::class])
class IosAppInfo : AppInfo {
  override fun getVersionName(): String {
    return NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: ""
  }

  override fun getVersionCode(): Long {
    val version = NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String ?: "0"
    return version.toLongOrNull() ?: 0L
  }
}
