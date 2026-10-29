package me.zhangls.about.mvi

import me.zhangls.framework.mvi.MviAction

internal sealed interface AboutAction : MviAction {
  data class SetVersionInfoExpanded(val expanded: Boolean) : AboutAction
}
