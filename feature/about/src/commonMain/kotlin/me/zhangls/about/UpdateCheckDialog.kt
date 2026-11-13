package me.zhangls.about

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import me.zhangls.about.mvi.UpdateStatus
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_check_updates
import notes.feature.about.generated.resources.about_release_dialog_dismiss
import notes.feature.about.generated.resources.about_update_available
import notes.feature.about.generated.resources.about_update_checking
import notes.feature.about.generated.resources.about_update_current
import notes.feature.about.generated.resources.about_update_failed
import notes.feature.about.generated.resources.about_update_open
import notes.feature.about.generated.resources.about_update_retry
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UpdateCheckDialog(status: UpdateStatus, onDismiss: () -> Unit, onRetry: () -> Unit) {
  val openInBrowser = rememberOpenInBrowser()
  val message = when (status) {
    UpdateStatus.Idle -> return
    UpdateStatus.Checking -> stringResource(Res.string.about_update_checking)
    UpdateStatus.Current -> stringResource(Res.string.about_update_current)
    is UpdateStatus.Failed -> stringResource(Res.string.about_update_failed)
    is UpdateStatus.Available -> stringResource(Res.string.about_update_available, status.manifest.version)
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.about_check_updates)) },
    text = { Text(message) },
    dismissButton = {
      if (status is UpdateStatus.Failed || status is UpdateStatus.Available) {
        TextButton(onClick = onDismiss) { Text(stringResource(Res.string.about_release_dialog_dismiss)) }
      }
    },
    confirmButton = {
      when (status) {
        is UpdateStatus.Failed -> TextButton(onClick = onRetry) { Text(stringResource(Res.string.about_update_retry)) }

        is UpdateStatus.Available -> TextButton(onClick = {
          openInBrowser(status.manifest.updateUrl)
          onDismiss()
        }) { Text(stringResource(Res.string.about_update_open)) }

        UpdateStatus.Current, UpdateStatus.Checking ->
          TextButton(onClick = onDismiss) { Text(stringResource(Res.string.about_release_dialog_dismiss)) }

        UpdateStatus.Idle -> Unit
      }
    },
  )
}
