package me.zhangls.about.mvi

import me.zhangls.framework.mvi.MviIntent

internal sealed interface AboutIntent : MviIntent {
  data object ToggleVersionInfo : AboutIntent
  data object CheckForUpdates : AboutIntent
  data object UpdateAction : AboutIntent
  data object DismissUpdateResult : AboutIntent
}
