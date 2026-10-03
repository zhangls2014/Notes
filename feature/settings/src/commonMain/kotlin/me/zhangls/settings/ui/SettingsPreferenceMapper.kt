package me.zhangls.settings.ui

import me.zhangls.data.model.SettingsModel
import me.zhangls.preference.AboutPreference
import me.zhangls.preference.DarkThemePreference
import me.zhangls.preference.DynamicColorPreference
import me.zhangls.preference.FontSizePreference
import me.zhangls.preference.LanguagePreference
import me.zhangls.preference.LogoutPreference
import me.zhangls.preference.PreferenceSpec
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
 * 全部设置项的表现层模型，顺序即展示顺序。
 *
 * @param sendIntent Intent 入口；消费方的 SettingsIntent 类型不会进入 core:preference。
 * @param dynamicColorSupported 是否展示动态取色项，默认使用平台能力位；测试可覆盖两种清单。
 */
internal fun SettingsModel.toPreferenceUiModels(
  sendIntent: (SettingsIntent) -> Unit,
  dynamicColorSupported: Boolean = supportsDynamicColor,
): List<PreferenceUiModel> = buildList {
  add(actionPreference(ProfilePreference.spec, SettingsIntent.OpenProfile, sendIntent))
  if (dynamicColorSupported) {
    add(
      PreferenceUiModel.Toggle(
        spec = DynamicColorPreference.spec,
        value = dynamicColor,
        onValueChange = { sendIntent(SettingsIntent.UpdateDynamicColor(it)) },
      ),
    )
  }
  add(selectPreference(DarkThemePreference.spec, darkTheme, SettingsIntent::UpdateDarkTheme, sendIntent))
  add(selectPreference(FontSizePreference.spec, fontSize, SettingsIntent::UpdateFontSize, sendIntent))
  add(selectPreference(LanguagePreference.spec, appLanguage, SettingsIntent::UpdateAppLanguage, sendIntent))
  add(actionPreference(AboutPreference.spec, SettingsIntent.OpenAbout, sendIntent))
  add(actionPreference(LogoutPreference.spec, SettingsIntent.ClickLogout, sendIntent))
}

private fun actionPreference(
  spec: PreferenceSpec.Action,
  intent: SettingsIntent,
  sendIntent: (SettingsIntent) -> Unit,
): PreferenceUiModel.Action = PreferenceUiModel.Action(
  spec = spec,
  onClick = { sendIntent(intent) },
)

private fun <T> selectPreference(
  spec: PreferenceSpec.Select<T>,
  value: T,
  intent: (T) -> SettingsIntent,
  sendIntent: (SettingsIntent) -> Unit,
): PreferenceUiModel.Select<T> = PreferenceUiModel.Select(
  spec = spec,
  value = value,
  onValueChange = { sendIntent(intent(it)) },
)
