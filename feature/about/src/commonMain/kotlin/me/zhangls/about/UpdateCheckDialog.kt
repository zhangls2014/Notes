package me.zhangls.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.zhangls.about.mvi.UpdateStatus
import me.zhangls.about.ui.ReleaseChanges
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_check_updates
import notes.feature.about.generated.resources.about_release_current_empty
import notes.feature.about.generated.resources.about_release_dialog_dismiss
import notes.feature.about.generated.resources.about_update_available
import notes.feature.about.generated.resources.about_update_current
import notes.feature.about.generated.resources.about_update_download
import notes.feature.about.generated.resources.about_update_download_failed
import notes.feature.about.generated.resources.about_update_downloading
import notes.feature.about.generated.resources.about_update_failed
import notes.feature.about.generated.resources.about_update_install
import notes.feature.about.generated.resources.about_update_install_failed
import notes.feature.about.generated.resources.about_update_install_permission
import notes.feature.about.generated.resources.about_update_older_downloading
import notes.feature.about.generated.resources.about_update_ready
import notes.feature.about.generated.resources.about_update_retry
import notes.feature.about.generated.resources.about_update_unavailable
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun UpdateCheckDialog(
  status: UpdateStatus,
  onDismiss: () -> Unit,
  onRetry: () -> Unit,
  onUpdateAction: () -> Unit,
) {
  val message = updateMessage(status) ?: return
  val downloading = status is UpdateStatus.Available && status.download == DownloadStatus.Downloading
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.about_check_updates)) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(message)
        if (status is UpdateStatus.Available && !downloading) {
          UpdateDetails(status)
        }
      }
    },
    dismissButton = {
      if (status is UpdateStatus.Failed || (status is UpdateStatus.Available && !downloading)) {
        TextButton(onClick = onDismiss) { Text(stringResource(Res.string.about_release_dialog_dismiss)) }
      }
    },
    confirmButton = {
      when (status) {
        is UpdateStatus.Failed -> Button(onClick = onRetry, shapes = ButtonDefaults.shapes()) {
          Text(stringResource(Res.string.about_update_retry))
        }

        is UpdateStatus.Available -> {
          if (downloading) {
            UpdateDismissButton(onDismiss)
          } else {
            UpdateActionButton(status, onUpdateAction)
          }
        }

        UpdateStatus.Current -> UpdateDismissButton(onDismiss)

        UpdateStatus.Idle, UpdateStatus.Checking -> Unit
      }
    },
  )
}

@Composable
private fun updateMessage(status: UpdateStatus): String? = when (status) {
  UpdateStatus.Idle, UpdateStatus.Checking -> null

  UpdateStatus.Current -> stringResource(Res.string.about_update_current)

  is UpdateStatus.Failed -> stringResource(Res.string.about_update_failed)

  is UpdateStatus.Available -> when (status.download) {
    DownloadStatus.Downloading ->
      if (status.showDownloadStatus) stringResource(Res.string.about_update_downloading) else null

    DownloadStatus.Ready -> stringResource(Res.string.about_update_ready, status.manifest.version)

    else -> stringResource(Res.string.about_update_available, status.manifest.version)
  }
}

@Composable
private fun UpdateDismissButton(onDismiss: () -> Unit) {
  TextButton(onClick = onDismiss) { Text(stringResource(Res.string.about_release_dialog_dismiss)) }
}

@Composable
private fun UpdateDetails(status: UpdateStatus.Available) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    if (status.download != DownloadStatus.Ready) {
      if (status.release == null) {
        Text(stringResource(Res.string.about_release_current_empty))
      } else {
        ReleaseChanges(status.release)
      }
    }
    if (status.olderDownloadActive && status.download != DownloadStatus.Ready) {
      Text(stringResource(Res.string.about_update_older_downloading))
    }
    if (status.download == DownloadStatus.Failed) {
      Text(stringResource(Res.string.about_update_download_failed))
    }
    when (status.installResult) {
      InstallResult.PermissionRequired -> Text(stringResource(Res.string.about_update_install_permission))
      InstallResult.Failed -> Text(stringResource(Res.string.about_update_install_failed))
      else -> Unit
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateActionButton(status: UpdateStatus.Available, onUpdateAction: () -> Unit) {
  val downloadable = status.apkUrl != null
  val label = when {
    !downloadable -> Res.string.about_update_unavailable
    status.download == DownloadStatus.Ready -> Res.string.about_update_install
    else -> Res.string.about_update_download
  }
  Button(onClick = onUpdateAction, enabled = downloadable && !status.working, shapes = ButtonDefaults.shapes()) {
    Text(stringResource(label))
  }
}
