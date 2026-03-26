package me.zhangls.settings.domain

import me.zhangls.data.model.SettingsModel
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.data.type.AppLanguage
import me.zhangls.data.type.DarkThemeConfig
import me.zhangls.data.type.FontSizeConfig
import me.zhangls.framework.mvi.DialogResult
import me.zhangls.framework.mvi.DialogState
import me.zhangls.framework.mvi.MviEffect
import me.zhangls.settings.R
import me.zhangls.settings.SettingsResult
import me.zhangls.settings.icon.DarkMode
import me.zhangls.settings.icon.FormatSize
import me.zhangls.settings.icon.Language
import me.zhangls.settings.icon.Palette
import me.zhangls.theme.icon.Icons

/**
 * @author zhangls
 */
class SettingsHandler(
  private val userRepository: UserRepository,
  private val settingsRepository: SettingsRepository,
) {
  sealed interface ClickAction {
    data object None : ClickAction

    data class ShowDialog(val dialog: DialogState) : ClickAction

    data class EmitEffect(val effect: MviEffect) : ClickAction
  }

  companion object {
    private const val KEY_DYNAMIC_COLOR = "dynamicColor"
    private const val KEY_DARK_THEME = "darkTheme"
    private const val KEY_FONT_SIZE = "fontSize"
    private const val KEY_APP_LANGUAGE = "appLanguage"
    private const val KEY_LOGOUT = "logout"

    private const val DIALOG_ID_LOGOUT = "dialog_id_logout"


    fun dynamicColorPreference(dynamicColor: Boolean): Preference.Switch {
      return Preference.Switch(
        key = KEY_DYNAMIC_COLOR,
        value = dynamicColor,
        title = R.string.settings_label_dynamic_color,
        summary = if (dynamicColor) {
          R.string.settings_msg_dynamic_color_on
        } else {
          R.string.settings_msg_dynamic_color_off
        },
        icon = Icons.Rounded.Palette,
      )
    }

    fun darkThemePreference(darkTheme: DarkThemeConfig): Preference.Alert<DarkThemeConfig> {
      return Preference.Alert(
        key = KEY_DARK_THEME,
        value = darkTheme,
        title = R.string.settings_label_dark_theme,
        summary = when (darkTheme) {
          DarkThemeConfig.FOLLOW_SYSTEM -> R.string.settings_label_dark_theme_follow_system
          DarkThemeConfig.LIGHT -> R.string.settings_label_dark_theme_light
          DarkThemeConfig.DARK -> R.string.settings_label_dark_theme_dark
        },
        options = listOf(
          Preference.Option(R.string.settings_label_dark_theme_follow_system, DarkThemeConfig.FOLLOW_SYSTEM),
          Preference.Option(R.string.settings_label_dark_theme_light, DarkThemeConfig.LIGHT),
          Preference.Option(R.string.settings_label_dark_theme_dark, DarkThemeConfig.DARK)
        ),
        icon = Icons.Rounded.DarkMode,
      )
    }

    fun fontSizePreference(fontSize: FontSizeConfig): Preference.Alert<FontSizeConfig> {
      return Preference.Alert(
        key = KEY_FONT_SIZE,
        value = fontSize,
        title = R.string.settings_label_font_size,
        summary = when (fontSize) {
          FontSizeConfig.STANDARD -> R.string.settings_label_font_size_standard
          FontSizeConfig.MEDIUM -> R.string.settings_label_font_size_medium
          FontSizeConfig.LARGE -> R.string.settings_label_font_size_large
        },
        options = listOf(
          Preference.Option(R.string.settings_label_font_size_standard, FontSizeConfig.STANDARD),
          Preference.Option(R.string.settings_label_font_size_medium, FontSizeConfig.MEDIUM),
          Preference.Option(R.string.settings_label_font_size_large, FontSizeConfig.LARGE)
        ),
        icon = Icons.Rounded.FormatSize,
      )
    }

    fun languagePreference(appLanguage: AppLanguage): Preference.Alert<AppLanguage> {
      return Preference.Alert(
        key = KEY_APP_LANGUAGE,
        value = appLanguage,
        title = R.string.settings_label_language,
        summary = when (appLanguage) {
          AppLanguage.FOLLOW_SYSTEM -> R.string.settings_label_language_follow_system
          AppLanguage.ENGLISH -> R.string.settings_label_language_english
          AppLanguage.CHINESE -> R.string.settings_label_language_chinese
        },
        options = listOf(
          Preference.Option(R.string.settings_label_language_follow_system, AppLanguage.FOLLOW_SYSTEM),
          Preference.Option(R.string.settings_label_language_english, AppLanguage.ENGLISH),
          Preference.Option(R.string.settings_label_language_chinese, AppLanguage.CHINESE)
        ),
        icon = Icons.Rounded.Language
      )
    }

    fun logoutPreference(): Preference.Text {
      return Preference.Text(
        key = KEY_LOGOUT,
        title = R.string.settings_label_logout,
        summary = null,
        icon = null,
      )
    }
  }

  fun mapToPreferences(settings: SettingsModel): List<Preference<*>> {
    return listOf(
      dynamicColorPreference(settings.dynamicColor),
      darkThemePreference(settings.darkTheme),
      fontSizePreference(settings.fontSize),
      languagePreference(settings.appLanguage),
      // 退出登录
      logoutPreference()
    )
  }

  suspend fun <T> updateSettings(key: String, result: T) {
    when (key) {
      KEY_DYNAMIC_COLOR -> {
        settingsRepository.updateDynamicColor(result as Boolean)
      }

      KEY_DARK_THEME -> {
        settingsRepository.updateDarkTheme(result as DarkThemeConfig)
      }

      KEY_FONT_SIZE -> {
        settingsRepository.updateFontSize(result as FontSizeConfig)
      }

      KEY_APP_LANGUAGE -> {
        settingsRepository.updateAppLanguage(result as AppLanguage)
      }
    }
  }

  fun checkClickSettings(key: String): ClickAction {
    when (key) {
      KEY_LOGOUT -> {
        return ClickAction.ShowDialog(createLogoutDialog())
      }
    }
    return ClickAction.None
  }

  private fun createLogoutDialog(): DialogState {
    return DialogState(
      dialogId = DIALOG_ID_LOGOUT,
      title = R.string.settings_dialog_label_logout,
      message = R.string.settings_dialog_content_logout,
      confirm = R.string.settings_dialog_action_logout,
      dismiss = R.string.settings_dialog_action_cancel
    )
  }

  suspend fun handleDialogCallback(result: DialogResult): MviEffect? {
    return when (result) {
      is DialogResult.Confirm -> {
        if (result.dialogId == DIALOG_ID_LOGOUT) {
          userRepository.clear()
          SettingsResult.Logout
        } else {
          null
        }
      }

      is DialogResult.Dismiss -> null
    }
  }
}
