package me.zhangls.about.mvi

import me.zhangls.framework.mvi.MviAction

internal sealed interface AboutAction : MviAction {
  data class SetBuildInfoExpanded(val expanded: Boolean) : AboutAction
}
