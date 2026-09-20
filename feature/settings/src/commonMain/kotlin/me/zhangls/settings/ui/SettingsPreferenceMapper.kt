package me.zhangls.settings.ui

import me.zhangls.data.model.SettingsModel
import me.zhangls.model.AppLanguage
import me.zhangls.model.DarkThemeConfig
import me.zhangls.model.FontSizeConfig
import me.zhangls.preference.DarkThemePreference
import me.zhangls.preference.DynamicColorPreference
import me.zhangls.preference.FontSizePreference
import me.zhangls.preference.LanguagePreference
import me.zhangls.preference.LogoutPreference
import me.zhangls.settings.mvi.SettingsIntent

/**
 * 把领域模型 [SettingsModel] 映射为设置页所需的表现层模型。
 *
 * 每一项都是「`core:preference` 的静态元数据 + 本模块的当前取值 + 本模块的 [SettingsIntent]」，
 * 本文件因此只负责**装配**，不再持有任何标题 / 图标 / 选项文案。
 *
 * 哪些设置项存在、以什么顺序展示由各平台的 actual 决定 —— 这是**平台相关**信息
 * （动态取色仅 Android 支持），因此不进 `core:preference`，必须留在本模块的 expect/actual 里。
 *
 * @author zhangls
 */
internal expect fun SettingsModel.toPreferenceUiModels(): List<PreferenceUiModel>

internal fun dynamicColorPreference(value: Boolean): PreferenceUiModel.Toggle {
  return PreferenceUiModel.Toggle(
    spec = DynamicColorPreference.spec,
    value = value,
    onValueChange = { SettingsIntent.UpdateDynamicColor(it) },
  )
}

internal fun darkThemePreference(value: DarkThemeConfig): PreferenceUiModel.Select<DarkThemeConfig> {
  return PreferenceUiModel.Select(
    spec = DarkThemePreference.spec,
    value = value,
    onValueChange = { SettingsIntent.UpdateDarkTheme(it) },
  )
}

internal fun fontSizePreference(value: FontSizeConfig): PreferenceUiModel.Select<FontSizeConfig> {
  return PreferenceUiModel.Select(
    spec = FontSizePreference.spec,
    value = value,
    onValueChange = { SettingsIntent.UpdateFontSize(it) },
  )
}

internal fun languagePreference(value: AppLanguage): PreferenceUiModel.Select<AppLanguage> {
  return PreferenceUiModel.Select(
    spec = LanguagePreference.spec,
    value = value,
    onValueChange = { SettingsIntent.UpdateAppLanguage(it) },
  )
}

internal fun logoutPreference(): PreferenceUiModel.Action {
  return PreferenceUiModel.Action(
    spec = LogoutPreference.spec,
    clickIntent = SettingsIntent.ClickLogout,
  )
}
