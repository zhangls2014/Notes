package me.zhangls.entry.util

/** 应用版本信息，由平台实现提供。 */
interface AppInfo {
  fun getVersionCode(): Long
  fun getVersionName(): String
}
