package me.zhangls.settings.domain

import me.zhangls.framework.mvi.DialogResult
import me.zhangls.framework.mvi.MviEffect
import me.zhangls.settings.api.SettingsResult
import org.koin.core.annotation.Singleton

/**
 * @author zhangls
 */
@Singleton
class SettingsHandler {
  fun createLogoutDialog(): SettingsDialog = SettingsDialog.Logout

  fun handleDialogCallback(result: DialogResult): MviEffect? = when (result) {
    is DialogResult.Confirm -> {
      if (result.dialogId == SettingsDialog.Logout.dialogId) {
        SettingsResult.Logout
      } else {
        null
      }
    }

    is DialogResult.Dismiss -> null
  }
}
