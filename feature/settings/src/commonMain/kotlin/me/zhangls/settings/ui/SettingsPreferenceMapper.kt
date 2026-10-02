package me.zhangls.settings.ui

import me.zhangls.data.model.SettingsModel
import me.zhangls.model.AppLanguage
import me.zhangls.model.DarkThemeConfig
import me.zhangls.model.FontSizeConfig
import me.zhangls.preference.DarkThemePreference
import me.zhangls.preference.AboutPreference
import me.zhangls.preference.DynamicColorPreference
import me.zhangls.preference.FontSizePreference
import me.zhangls.preference.LanguagePreference
import me.zhangls.preference.LogoutPreference
import me.zhangls.preference.ProfilePreference
import me.zhangls.preference.ui.PreferenceUiModel
import me.zhangls.settings.mvi.SettingsIntent

/**
 * 把领域模型 [SettingsModel] 映射为 `core:preference` 的表现层模型（`spec` + 取值 + 回调）。
 *
 * 本文件是设置页唯一的**装配层**，只做三件事：从 State 取当前值、把 [sendIntent] 绑到各项回调、
 * 按平台能力位决定清单。渲染（"这一项长什么样"）一行都不在本模块 —— 那是 `core:preference`
 * 的 `PreferenceRow`。这正是控件层下沉的收益：登录页与设置页展示同一批 spec，形态不同，
 * 但两边都不需要写渲染代码。
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
 *
 * @param sendIntent Intent 入口；在此处就把回调绑好，模型因此只携带 `(T) -> Unit`，
 *   消费方的 `SettingsIntent` 类型不会进入 `core:preference`
 */
internal fun SettingsModel.toPreferenceUiModels(
  sendIntent: (SettingsIntent) -> Unit,
): List<PreferenceUiModel> = buildList {
  add(PreferenceUiModel.Action(
    spec = ProfilePreference.spec,
    onClick = { sendIntent(SettingsIntent.OpenProfile) },
  ))
  if (supportsDynamicColor) {
    add(dynamicColorPreference(dynamicColor, sendIntent))
  }
  add(darkThemePreference(darkTheme, sendIntent))
  add(fontSizePreference(fontSize, sendIntent))
  add(languagePreference(appLanguage, sendIntent))
  add(PreferenceUiModel.Action(
    spec = AboutPreference.spec,
    onClick = { sendIntent(SettingsIntent.OpenAbout) },
  ))
  // 退出登录
  add(logoutPreference(sendIntent))
}

internal fun dynamicColorPreference(
  value: Boolean,
  sendIntent: (SettingsIntent) -> Unit,
): PreferenceUiModel.Toggle {
  return PreferenceUiModel.Toggle(
    spec = DynamicColorPreference.spec,
    value = value,
    onValueChange = { sendIntent(SettingsIntent.UpdateDynamicColor(it)) },
  )
}

internal fun darkThemePreference(
  value: DarkThemeConfig,
  sendIntent: (SettingsIntent) -> Unit,
): PreferenceUiModel.Select<DarkThemeConfig> {
  return PreferenceUiModel.Select(
    spec = DarkThemePreference.spec,
    value = value,
    onValueChange = { sendIntent(SettingsIntent.UpdateDarkTheme(it)) },
  )
}

internal fun fontSizePreference(
  value: FontSizeConfig,
  sendIntent: (SettingsIntent) -> Unit,
): PreferenceUiModel.Select<FontSizeConfig> {
  return PreferenceUiModel.Select(
    spec = FontSizePreference.spec,
    value = value,
    onValueChange = { sendIntent(SettingsIntent.UpdateFontSize(it)) },
  )
}

internal fun languagePreference(
  value: AppLanguage,
  sendIntent: (SettingsIntent) -> Unit,
): PreferenceUiModel.Select<AppLanguage> {
  return PreferenceUiModel.Select(
    spec = LanguagePreference.spec,
    value = value,
    onValueChange = { sendIntent(SettingsIntent.UpdateAppLanguage(it)) },
  )
}

internal fun logoutPreference(
  sendIntent: (SettingsIntent) -> Unit,
): PreferenceUiModel.Action {
  return PreferenceUiModel.Action(
    spec = LogoutPreference.spec,
    onClick = { sendIntent(SettingsIntent.ClickLogout) },
  )
}
