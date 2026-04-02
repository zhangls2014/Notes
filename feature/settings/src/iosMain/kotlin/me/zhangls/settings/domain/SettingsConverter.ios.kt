package me.zhangls.settings.domain

import me.zhangls.data.model.SettingsModel
import me.zhangls.settings.domain.SettingsHandler.Companion.darkThemePreference
import me.zhangls.settings.domain.SettingsHandler.Companion.fontSizePreference
import me.zhangls.settings.domain.SettingsHandler.Companion.languagePreference
import me.zhangls.settings.domain.SettingsHandler.Companion.logoutPreference


actual fun SettingsModel.mapToPreferences(): List<Preference<*>> {
  return listOf(
    darkThemePreference(darkTheme),
    fontSizePreference(fontSize),
    languagePreference(appLanguage),
    // 退出登录
    logoutPreference()
  )
}