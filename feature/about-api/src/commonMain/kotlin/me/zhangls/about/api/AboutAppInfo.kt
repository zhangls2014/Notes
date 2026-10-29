package me.zhangls.about.api

import kotlinx.serialization.Serializable

/** Installed package presentation metadata resolved from the injected platform source; null means unavailable. */
@Serializable
data class AboutAppInfo(
  val versionName: String?,
  val buildNumber: String?,
)
