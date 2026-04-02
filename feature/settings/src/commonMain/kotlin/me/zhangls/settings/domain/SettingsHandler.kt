package me.zhangls.settings.domain

import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.data.type.AppLanguage
import me.zhangls.data.type.DarkThemeConfig
import me.zhangls.data.type.FontSizeConfig
import me.zhangls.framework.mvi.DialogResult
import me.zhangls.framework.mvi.DialogState
import me.zhangls.framework.mvi.MviEffect
import me.zhangls.settings.SettingsResult
import me.zhangls.settings.icon.DarkMode
import me.zhangls.settings.icon.FormatSize
import me.zhangls.settings.icon.Language
import me.zhangls.settings.icon.Palette
import me.zhangls.theme.icon.Icons
import notes.feature.settings.generated.resources.Res
import notes.feature.settings.generated.resources.settings_dialog_action_cancel
import notes.feature.settings.generated.resources.settings_dialog_action_logout
import notes.feature.settings.generated.resources.settings_dialog_content_logout
import notes.feature.settings.generated.resources.settings_dialog_label_logout
import notes.feature.settings.generated.resources.settings_label_dark_theme
import notes.feature.settings.generated.resources.settings_label_dark_theme_dark
import notes.feature.settings.generated.resources.settings_label_dark_theme_follow_system
import notes.feature.settings.generated.resources.settings_label_dark_theme_light
import notes.feature.settings.generated.resources.settings_label_dynamic_color
import notes.feature.settings.generated.resources.settings_label_font_size
import notes.feature.settings.generated.resources.settings_label_font_size_large
import notes.feature.settings.generated.resources.settings_label_font_size_medium
import notes.feature.settings.generated.resources.settings_label_font_size_standard
import notes.feature.settings.generated.resources.settings_label_language
import notes.feature.settings.generated.resources.settings_label_language_chinese
import notes.feature.settings.generated.resources.settings_label_language_english
import notes.feature.settings.generated.resources.settings_label_language_follow_system
import notes.feature.settings.generated.resources.settings_label_logout
import notes.feature.settings.generated.resources.settings_msg_dynamic_color_off
import notes.feature.settings.generated.resources.settings_msg_dynamic_color_on
import org.jetbrains.compose.resources.getString

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
        title = Res.string.settings_label_dynamic_color,
        summary = if (dynamicColor) {
          Res.string.settings_msg_dynamic_color_on
        } else {
          Res.string.settings_msg_dynamic_color_off
        },
        icon = Icons.Rounded.Palette,
      )
    }

    fun darkThemePreference(darkTheme: DarkThemeConfig): Preference.Alert<DarkThemeConfig> {
      return Preference.Alert(
        key = KEY_DARK_THEME,
        value = darkTheme,
        title = Res.string.settings_label_dark_theme,
        summary = when (darkTheme) {
          DarkThemeConfig.FOLLOW_SYSTEM -> Res.string.settings_label_dark_theme_follow_system
          DarkThemeConfig.LIGHT -> Res.string.settings_label_dark_theme_light
          DarkThemeConfig.DARK -> Res.string.settings_label_dark_theme_dark
        },
        options = listOf(
          Preference.Option(Res.string.settings_label_dark_theme_follow_system, DarkThemeConfig.FOLLOW_SYSTEM),
          Preference.Option(Res.string.settings_label_dark_theme_light, DarkThemeConfig.LIGHT),
          Preference.Option(Res.string.settings_label_dark_theme_dark, DarkThemeConfig.DARK)
        ),
        icon = Icons.Rounded.DarkMode,
      )
    }

    fun fontSizePreference(fontSize: FontSizeConfig): Preference.Alert<FontSizeConfig> {
      return Preference.Alert(
        key = KEY_FONT_SIZE,
        value = fontSize,
        title = Res.string.settings_label_font_size,
        summary = when (fontSize) {
          FontSizeConfig.STANDARD -> Res.string.settings_label_font_size_standard
          FontSizeConfig.MEDIUM -> Res.string.settings_label_font_size_medium
          FontSizeConfig.LARGE -> Res.string.settings_label_font_size_large
        },
        options = listOf(
          Preference.Option(Res.string.settings_label_font_size_standard, FontSizeConfig.STANDARD),
          Preference.Option(Res.string.settings_label_font_size_medium, FontSizeConfig.MEDIUM),
          Preference.Option(Res.string.settings_label_font_size_large, FontSizeConfig.LARGE)
        ),
        icon = Icons.Rounded.FormatSize,
      )
    }

    fun languagePreference(appLanguage: AppLanguage): Preference.Alert<AppLanguage> {
      return Preference.Alert(
        key = KEY_APP_LANGUAGE,
        value = appLanguage,
        title = Res.string.settings_label_language,
        summary = when (appLanguage) {
          AppLanguage.FOLLOW_SYSTEM -> Res.string.settings_label_language_follow_system
          AppLanguage.ENGLISH -> Res.string.settings_label_language_english
          AppLanguage.CHINESE -> Res.string.settings_label_language_chinese
        },
        options = listOf(
          Preference.Option(Res.string.settings_label_language_follow_system, AppLanguage.FOLLOW_SYSTEM),
          Preference.Option(Res.string.settings_label_language_english, AppLanguage.ENGLISH),
          Preference.Option(Res.string.settings_label_language_chinese, AppLanguage.CHINESE)
        ),
        icon = Icons.Rounded.Language
      )
    }

    fun logoutPreference(): Preference.Text {
      return Preference.Text(
        key = KEY_LOGOUT,
        title = Res.string.settings_label_logout,
        summary = null,
        icon = null,
      )
    }
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

  suspend fun checkClickSettings(key: String): ClickAction {
    when (key) {
      KEY_LOGOUT -> {
        return ClickAction.ShowDialog(createLogoutDialog())
      }
    }
    return ClickAction.None
  }

  private suspend fun createLogoutDialog(): DialogState {
    return DialogState(
      dialogId = DIALOG_ID_LOGOUT,
      title = getString(Res.string.settings_dialog_label_logout),
      message = getString(Res.string.settings_dialog_content_logout),
      confirm = getString(Res.string.settings_dialog_action_logout),
      dismiss = getString(Res.string.settings_dialog_action_cancel),
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
