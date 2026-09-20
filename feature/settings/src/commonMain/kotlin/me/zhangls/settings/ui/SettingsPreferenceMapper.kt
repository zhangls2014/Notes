package me.zhangls.settings.ui

import me.zhangls.data.model.SettingsModel
import me.zhangls.model.AppLanguage
import me.zhangls.model.DarkThemeConfig
import me.zhangls.data.type.FontSizeConfig
import me.zhangls.settings.mvi.SettingsIntent
import me.zhangls.theme.ThemeColor
import me.zhangls.theme.darkmode.DarkThemeCatalog
import me.zhangls.theme.icon.ExitToApp
import me.zhangls.theme.icon.FormatSize
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Palette
import me.zhangls.theme.language.LanguageCatalog
import notes.feature.settings.generated.resources.Res
import notes.feature.settings.generated.resources.settings_label_dynamic_color
import notes.feature.settings.generated.resources.settings_label_font_size
import notes.feature.settings.generated.resources.settings_label_font_size_large
import notes.feature.settings.generated.resources.settings_label_font_size_medium
import notes.feature.settings.generated.resources.settings_label_font_size_standard
import notes.feature.settings.generated.resources.settings_label_logout
import notes.feature.settings.generated.resources.settings_msg_dynamic_color_off
import notes.feature.settings.generated.resources.settings_msg_dynamic_color_on

/**
 * 将领域模型 [SettingsModel] 映射为设置页所需的表现层模型。
 *
 * @author zhangls
 */
internal expect fun SettingsModel.toPreferenceUiModels(): List<PreferenceUiModel>

internal fun dynamicColorPreference(dynamicColor: Boolean): PreferenceUiModel.Switch {
  return PreferenceUiModel.Switch(
    key = "dynamicColor",
    value = dynamicColor,
    title = Res.string.settings_label_dynamic_color,
    summary = if (dynamicColor) {
      Res.string.settings_msg_dynamic_color_on
    } else {
      Res.string.settings_msg_dynamic_color_off
    },
    icon = Icons.Rounded.Palette,
    onValueChange = { SettingsIntent.UpdateDynamicColor(it) },
  )
}

internal fun darkThemePreference(darkTheme: DarkThemeConfig): PreferenceUiModel.Alert<DarkThemeConfig> {
  return PreferenceUiModel.Alert(
    key = "darkTheme",
    value = darkTheme,
    title = DarkThemeCatalog.title,
    summary = DarkThemeCatalog.summary(darkTheme),
    options = DarkThemeCatalog.options.map { PreferenceUiModel.Option(it.label, it.value) },
    icon = DarkThemeCatalog.icon,
    onValueChange = { SettingsIntent.UpdateDarkTheme(it) },
  )
}

internal fun fontSizePreference(fontSize: FontSizeConfig): PreferenceUiModel.Alert<FontSizeConfig> {
  return PreferenceUiModel.Alert(
    key = "fontSize",
    value = fontSize,
    title = Res.string.settings_label_font_size,
    summary = when (fontSize) {
      FontSizeConfig.STANDARD -> Res.string.settings_label_font_size_standard
      FontSizeConfig.MEDIUM -> Res.string.settings_label_font_size_medium
      FontSizeConfig.LARGE -> Res.string.settings_label_font_size_large
    },
    options = listOf(
      PreferenceUiModel.Option(Res.string.settings_label_font_size_standard, FontSizeConfig.STANDARD),
      PreferenceUiModel.Option(Res.string.settings_label_font_size_medium, FontSizeConfig.MEDIUM),
      PreferenceUiModel.Option(Res.string.settings_label_font_size_large, FontSizeConfig.LARGE)
    ),
    icon = Icons.Rounded.FormatSize,
    onValueChange = { SettingsIntent.UpdateFontSize(it) },
  )
}

internal fun languagePreference(appLanguage: AppLanguage): PreferenceUiModel.Alert<AppLanguage> {
  return PreferenceUiModel.Alert(
    key = "appLanguage",
    value = appLanguage,
    title = LanguageCatalog.title,
    summary = LanguageCatalog.summary(appLanguage),
    options = LanguageCatalog.options.map { PreferenceUiModel.Option(it.label, it.value) },
    icon = LanguageCatalog.icon,
    onValueChange = { SettingsIntent.UpdateAppLanguage(it) },
  )
}

internal fun logoutPreference(): PreferenceUiModel.Text {
  return PreferenceUiModel.Text(
    key = "logout",
    title = Res.string.settings_label_logout,
    summary = null,
    icon = Icons.Rounded.ExitToApp,
    tint = ThemeColor.Error,
    clickIntent = SettingsIntent.ClickLogout,
  )
}
