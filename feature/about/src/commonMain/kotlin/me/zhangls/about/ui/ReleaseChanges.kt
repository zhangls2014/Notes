package me.zhangls.about.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import me.zhangls.about.AppRelease
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_release_added
import notes.feature.about.generated.resources.about_release_fixed
import notes.feature.about.generated.resources.about_release_improved
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReleaseChanges(release: AppRelease) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    for ((type, changes) in release.changes.groupBy { it.type }) {
      val label = when (type) {
        "added" -> Res.string.about_release_added
        "improved" -> Res.string.about_release_improved
        else -> Res.string.about_release_fixed
      }
      Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
      for (change in changes) {
        Text("• ${change.localizedText(Locale.current.language)}")
      }
    }
  }
}
