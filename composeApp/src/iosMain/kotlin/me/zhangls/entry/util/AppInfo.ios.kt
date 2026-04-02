package me.zhangls.entry.util

import platform.Foundation.NSBundle


actual object AppInfo {
  actual fun getVersionName(): String {
    return NSBundle.mainBundle.infoDictionary?.get("CFBundleShortVersionString") as? String ?: ""
  }

  actual fun getVersionCode(): Long {
    val version = NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion") as? String ?: "0"
    return version.toLongOrNull() ?: 0L
  }
}