package me.zhangls.about.mvi

import kotlinx.serialization.Serializable
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.framework.mvi.MviState

@Serializable
internal data class AboutState(
  val appInfo: AboutAppInfo,
  val isBuildInfoExpanded: Boolean = false,
) : MviState
