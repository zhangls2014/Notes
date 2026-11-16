package me.zhangls.web

import kotlinx.serialization.Serializable

@Serializable
internal data class SiteData(val builds: List<PublishedBuild>)

@Serializable
internal data class Release(val version: String, val date: String, val changes: List<ReleaseChange>)

@Serializable
internal data class ReleaseChange(val type: String, val platforms: List<String>, val text: Map<String, String>)

@Serializable
internal data class PublishedBuild(
  val channel: String,
  val version: String,
  val build: String,
  val artifactUrl: String? = null,
) {
  val distributionChannel: String get() = if (channel == "android-prd-full") "android-prd-global" else channel

  val download: String?
    get() = artifactUrl?.let { if (channel == "ios-app-store") it else "/downloads/android/$channel/$build/" }

  fun label(chinese: Boolean): String = when (channel) {
    "android-prd-full", "android-prd-global" -> if (chinese) "Android 国际版" else "Android Global"
    "android-prd-mainland" -> if (chinese) "Android 中国大陆版" else "Android Mainland China"
    else -> "iOS"
  }
}
