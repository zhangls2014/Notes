package me.zhangls.settings.domain

import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.DialogResult
import me.zhangls.framework.mvi.DialogState
import me.zhangls.framework.mvi.MviEffect
import me.zhangls.settings.api.SettingsResult
import notes.feature.settings.generated.resources.Res
import notes.feature.settings.generated.resources.settings_dialog_action_cancel
import notes.feature.settings.generated.resources.settings_dialog_action_logout
import notes.feature.settings.generated.resources.settings_dialog_content_logout
import notes.feature.settings.generated.resources.settings_dialog_label_logout
import org.jetbrains.compose.resources.getString

/**
 * @author zhangls
 */
class SettingsHandler(
  private val userRepository: UserRepository,
) {
  companion object {
    private const val DIALOG_ID_LOGOUT = "dialog_id_logout"
  }

  suspend fun createLogoutDialog(): DialogState {
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
