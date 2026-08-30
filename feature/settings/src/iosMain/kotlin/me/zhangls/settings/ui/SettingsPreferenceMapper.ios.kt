package me.zhangls.settings.ui

import me.zhangls.data.model.SettingsModel

// iOS 不支持动态取色
internal actual fun SettingsModel.toPreferenceUiModels(): List<PreferenceUiModel> {
  return listOf(
    darkThemePreference(darkTheme),
    fontSizePreference(fontSize),
    languagePreference(appLanguage),
    // 退出登录
    logoutPreference()
  )
}
