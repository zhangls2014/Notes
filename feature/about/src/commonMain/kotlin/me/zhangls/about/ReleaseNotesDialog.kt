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
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_release_dialog_dismiss
import notes.feature.about.generated.resources.about_release_dialog_title
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        for (change in release.changes) {
          Text("• ${change.localizedText(Locale.current.language)}")
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(Res.string.about_release_dialog_dismiss)) }
    },
  )
}
