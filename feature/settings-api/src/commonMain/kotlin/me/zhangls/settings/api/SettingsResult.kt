package me.zhangls.settings.api

import me.zhangls.framework.mvi.MviEffect

/**
 * @author zhangls
 */
sealed interface SettingsResult : MviEffect {
  data object Done : SettingsResult
  data object Logout : SettingsResult
}
