package me.zhangls.data.repository

import kotlinx.coroutines.flow.Flow
import me.zhangls.data.model.SettingsModel
import me.zhangls.data.type.FontSizeConfig
import me.zhangls.model.AppLanguage
import me.zhangls.model.DarkThemeConfig

interface SettingsRepository {
  val settingsFlow: Flow<SettingsModel>

  suspend fun updateDarkTheme(darkThemeConfig: DarkThemeConfig)

  suspend fun updateDynamicColor(useDynamicColor: Boolean)

  suspend fun updateFontSize(fontSizeConfig: FontSizeConfig)

  suspend fun updateAppLanguage(appLanguage: AppLanguage)
}
