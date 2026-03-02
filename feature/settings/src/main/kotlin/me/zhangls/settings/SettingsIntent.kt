package me.zhangls.settings

import me.zhangls.framework.mvi.MviIntent

/**
 * @author zhangls
 */
sealed interface SettingsIntent : MviIntent {
  data class UpdateSettings<T>(val key: String, val result: T) : SettingsIntent
}