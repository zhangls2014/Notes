package me.zhangls.settings.domain

import me.zhangls.data.model.SettingsModel
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.data.type.DarkThemeConfig
import me.zhangls.framework.mvi.DialogState
import me.zhangls.framework.mvi.DialogResult
import me.zhangls.framework.mvi.MviEffect
import me.zhangls.settings.R
import me.zhangls.settings.SettingsResult

/**
 * @author zhangls
 */
class SettingsHandler(
  private val userRepository: UserRepository,
  private val settingsRepository: SettingsRepository,
) {
  companion object {
    private const val KEY_DYNAMIC_COLOR = "dynamicColor"
    private const val KEY_DARK_THEME = "darkTheme"
    private const val KEY_LOGOUT = "logout"

    private const val DIALOG_ID_LOGOUT = "dialog_id_logout"
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

      // 退出登录
      Preference.Text(
        key = KEY_LOGOUT,
        title = R.string.settings_label_logout,
        summary = null
      )
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
    }
  }

  fun checkClickSettings(key: String): DialogState? {
    when (key) {
      KEY_LOGOUT -> {
        return createLogoutDialog()
      }
    }
    return null
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

  suspend fun handleDialogCallback(result: DialogResult, callback: (MviEffect) -> Unit) {
    when (result) {
      is DialogResult.Confirm -> {
        if (result.dialogId == DIALOG_ID_LOGOUT) {
          userRepository.clear()
          callback(SettingsResult.Logout)
        }
      }

      is DialogResult.Dismiss -> {}
    }
  }
}
