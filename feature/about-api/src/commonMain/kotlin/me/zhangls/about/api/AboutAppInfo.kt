package me.zhangls.about.api

import kotlinx.serialization.Serializable

/** Installed package metadata supplied by the host; null means unavailable. */
@Serializable
data class AboutAppInfo(
  val versionName: String?,
  val buildNumber: String?,
  val fontScale: Float = 1f,
  val darkTheme: Boolean = false,
  val pageColors: NoticesColors? = null,
)

/** Resolved app colors as six-digit RGB hex strings, without a leading '#'. */
@Serializable
data class NoticesColors(
  val background: String,
  val card: String,
  val foreground: String,
  val muted: String,
  val outline: String,
  val accent: String,
  val primaryContainer: String? = null,
  val onPrimaryContainer: String? = null,
)
