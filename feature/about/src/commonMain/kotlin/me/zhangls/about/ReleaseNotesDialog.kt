package me.zhangls.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.zhangls.about.ui.ReleaseChanges
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_release_dialog_dismiss
import notes.feature.about.generated.resources.about_release_dialog_title
import notes.feature.about.generated.resources.about_release_unreleased
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReleaseNotesDialog(version: String, onDismiss: () -> Unit) {
  val result by rememberReleaseHistory()
  val release = result?.getOrNull()?.find { it.version == version } ?: return
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.about_release_dialog_title, version)) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(release.date ?: stringResource(Res.string.about_release_unreleased))
        ReleaseChanges(release)
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(Res.string.about_release_dialog_dismiss)) }
    },
  )
}
