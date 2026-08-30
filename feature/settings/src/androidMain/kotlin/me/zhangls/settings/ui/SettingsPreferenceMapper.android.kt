package me.zhangls.settings.ui

import me.zhangls.data.model.SettingsModel

internal actual fun SettingsModel.toPreferenceUiModels(): List<PreferenceUiModel> {
  return listOf(
    dynamicColorPreference(dynamicColor),
    darkThemePreference(darkTheme),
    fontSizePreference(fontSize),
    languagePreference(appLanguage),
    // 退出登录
    logoutPreference()
  )
}
