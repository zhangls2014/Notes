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
 * 设置项清单（有哪些、什么顺序）只在 common 定义一次，平台差异用能力位 [supportsDynamicColor]
 * 过滤后表达。这类"平台相关"信息确实不该进 `core:preference`（动态取色仅 Android 支持），
 * 但也不该让各平台各写一份清单 —— 两份清单的问题不是重复，而是**必然漏改一侧**。
 *
 * @author zhangls
 */
internal expect val supportsDynamicColor: Boolean

/**
 * 全部设置项的表现层模型，顺序即展示顺序。[supportsDynamicColor] 为 false 的平台会跳过动态取色项。
 */
internal fun SettingsModel.toPreferenceUiModels(): List<PreferenceUiModel> = buildList {
  if (supportsDynamicColor) {
    add(dynamicColorPreference(dynamicColor))
  }
  add(darkThemePreference(darkTheme))
  add(fontSizePreference(fontSize))
  add(languagePreference(appLanguage))
  // 退出登录
  add(logoutPreference())
}

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
