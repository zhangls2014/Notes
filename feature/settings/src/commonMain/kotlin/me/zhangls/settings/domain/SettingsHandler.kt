package me.zhangls.settings.domain

import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.DialogResult
import me.zhangls.framework.mvi.MviEffect
import me.zhangls.settings.api.SettingsResult

/**
 * @author zhangls
 */
class SettingsHandler(
  private val userRepository: UserRepository,
) {
  fun createLogoutDialog(): SettingsDialog {
    return SettingsDialog.Logout
  }

  suspend fun handleDialogCallback(result: DialogResult): MviEffect? {
    return when (result) {
      is DialogResult.Confirm -> {
        if (result.dialogId == SettingsDialog.Logout.dialogId) {
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
