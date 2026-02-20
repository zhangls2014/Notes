package me.zhangls.notes.ui.logout

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.zhangls.notes.R
import me.zhangls.theme.component.SimpleDialog

/**
 * @author zhangls
 */
@Composable
fun LogoutDialog(onLogoutResult: (LogoutResult) -> Unit) {
  SimpleDialog(
    title = stringResource(R.string.app_dialog_label_logout),
    content = stringResource(R.string.app_dialog_content_logout),
    confirmText = stringResource(R.string.app_dialog_action_logout),
    confirm = {
      onLogoutResult(LogoutResult.Logout)
    },
    dismissText = stringResource(R.string.app_dialog_action_cancel),
    dismiss = {
      onLogoutResult(LogoutResult.Cancel)
    }
  )
}

