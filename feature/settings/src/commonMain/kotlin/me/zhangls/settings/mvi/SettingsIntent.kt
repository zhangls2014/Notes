package me.zhangls.settings.mvi

import me.zhangls.data.type.AppLanguage
import me.zhangls.data.type.DarkThemeConfig
import me.zhangls.data.type.FontSizeConfig
import me.zhangls.framework.mvi.DialogResult
import me.zhangls.framework.mvi.MviIntent

/**
 * @author zhangls
 */
sealed interface SettingsIntent : MviIntent {
  data class UpdateDynamicColor(val value: Boolean) : SettingsIntent

  data class UpdateDarkTheme(val value: DarkThemeConfig) : SettingsIntent

  data class UpdateFontSize(val value: FontSizeConfig) : SettingsIntent

  data class UpdateAppLanguage(val value: AppLanguage) : SettingsIntent

  data object ClickLogout : SettingsIntent

  data class DialogCallback(val result: DialogResult) : SettingsIntent
}
