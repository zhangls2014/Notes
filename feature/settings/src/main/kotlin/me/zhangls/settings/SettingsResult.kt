package me.zhangls.settings

import me.zhangls.framework.mvi.MviEffect

/**
 * @author zhangls
 */
sealed interface SettingsResult : MviEffect {
  data object Done : SettingsResult
}