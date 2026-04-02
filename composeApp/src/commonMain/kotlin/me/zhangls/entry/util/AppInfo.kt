package me.zhangls.entry.util


expect object AppInfo {
  fun getVersionCode(): Long
  fun getVersionName(): String
}