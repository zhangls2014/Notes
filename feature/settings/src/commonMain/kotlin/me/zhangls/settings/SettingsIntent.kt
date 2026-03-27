package me.zhangls.settings

import me.zhangls.framework.mvi.DialogResult
import me.zhangls.framework.mvi.MviIntent

/**
 * @author zhangls
 */
sealed interface SettingsIntent : MviIntent {
  data class UpdateSettings<T>(val key: String, val result: T) : SettingsIntent

  data class ClickSettings(val key: String) : SettingsIntent

  data class DialogCallback(val result: DialogResult) : SettingsIntent
}