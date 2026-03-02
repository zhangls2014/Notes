package me.zhangls.settings.domain

import me.zhangls.data.model.DarkThemeConfig
import me.zhangls.data.model.SettingsModel
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.settings.R

/**
 * @author zhangls
 */
class SettingsConverter {
  companion object {
    private const val KEY_DYNAMIC_COLOR = "dynamicColor"
    private const val KEY_DARK_THEME = "darkTheme"
  }

  fun mapToPreferences(settings: SettingsModel): List<Preference<*>> {
    return listOf(
      Preference.Switch(
        key = KEY_DYNAMIC_COLOR,
        value = settings.dynamicColor,
        title = R.string.settings_label_dynamic_color,
        summary = if (settings.dynamicColor) {
          R.string.settings_msg_dynamic_color_on
        } else {
          R.string.settings_msg_dynamic_color_off
        }
      ),

      Preference.Alert(
        key = KEY_DARK_THEME,
        value = settings.darkTheme,
        title = R.string.settings_label_dark_theme,
        summary = when (settings.darkTheme) {
          DarkThemeConfig.FOLLOW_SYSTEM -> R.string.settings_label_dark_theme_follow_system
          DarkThemeConfig.LIGHT -> R.string.settings_label_dark_theme_light
          DarkThemeConfig.DARK -> R.string.settings_label_dark_theme_dark
        },
        options = listOf(
          Preference.Option(R.string.settings_label_dark_theme_follow_system, DarkThemeConfig.FOLLOW_SYSTEM),
          Preference.Option(R.string.settings_label_dark_theme_light, DarkThemeConfig.LIGHT),
          Preference.Option(R.string.settings_label_dark_theme_dark, DarkThemeConfig.DARK)
        )
      ),
    )
  }

  suspend fun <T> updateSettings(settingsRepository: SettingsRepository, key: String, result: T) {
    when (key) {
      KEY_DYNAMIC_COLOR -> {
        settingsRepository.updateDynamicColor(result as Boolean)
      }

      KEY_DARK_THEME -> {
        settingsRepository.updateDarkTheme(result as DarkThemeConfig)
      }
    }
  }
}