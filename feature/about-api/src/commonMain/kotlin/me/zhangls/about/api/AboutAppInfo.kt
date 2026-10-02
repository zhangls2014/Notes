package me.zhangls.about.api

import kotlinx.serialization.Serializable

/** Installed package metadata supplied by the host; null means unavailable. */
@Serializable
data class AboutAppInfo(val versionName: String?, val buildNumber: String?)
