package me.zhangls.about.mvi

import me.zhangls.framework.mvi.MviIntent

internal sealed interface AboutIntent : MviIntent {
  data object ToggleBuildInfo : AboutIntent
}
