package me.zhangls.about.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import me.zhangls.about.rememberReleaseHistory
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_release_contents
import notes.feature.about.generated.resources.about_release_current_empty
import notes.feature.about.generated.resources.about_release_failed
import notes.feature.about.generated.resources.about_release_loading
import notes.feature.about.generated.resources.about_release_unreleased
import notes.feature.about.generated.resources.about_release_view_history
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AboutReleaseSummary(version: String?, shape: Shape, onOpenHistory: () -> Unit) {
  val result by rememberReleaseHistory()
  Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(stringResource(Res.string.about_release_contents), style = MaterialTheme.typography.titleMedium)
      val loaded = result
      val release = loaded?.getOrNull()?.find { it.version == version }
      when {
        loaded == null -> Text(stringResource(Res.string.about_release_loading))

        loaded.isFailure -> Text(stringResource(Res.string.about_release_failed))

        release == null -> Text(stringResource(Res.string.about_release_current_empty))

        else -> {
          Text(release.date ?: stringResource(Res.string.about_release_unreleased))
          for (change in release.changes) {
            Text("• ${change.localizedText(Locale.current.language)}")
          }
        }
      }
      TextButton(onClick = onOpenHistory) { Text(stringResource(Res.string.about_release_view_history)) }
    }
  }
}
