package me.zhangls.framework

/** 应用版本信息，由平台实现提供。 */
interface AppInfo {
  fun getVersionCode(): Long
  fun getVersionName(): String

  /** Distribution channel for builds with published update manifests. */
  fun getUpdateChannel(): String? = null

  /** Raw platform build identifier; iOS can use a dotted string. */
  fun getBuildNumber(): String?
}
