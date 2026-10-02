package me.zhangls.about.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.theme.icon.ArrowBackIosNew
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Info
import me.zhangls.theme.icon.Refresh
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_build_number
import notes.feature.about.generated.resources.about_check_updates
import notes.feature.about.generated.resources.about_collapsed
import notes.feature.about.generated.resources.about_expanded
import notes.feature.about.generated.resources.about_coming_soon
import notes.feature.about.generated.resources.about_unknown
import notes.feature.about.generated.resources.about_update_description
import notes.feature.about.generated.resources.about_version
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AboutVersionGroup(appInfo: AboutAppInfo, expanded: Boolean, onToggleBuildInfo: () -> Unit) {
  val outer = MaterialTheme.shapes.extraLarge
  val inner = MaterialTheme.shapes.extraSmall
  val versionShape = outer.copy(bottomStart = inner.bottomStart, bottomEnd = inner.bottomEnd)
  val stateLabel = stringResource(if (expanded) Res.string.about_expanded else Res.string.about_collapsed)
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    AboutInfoRow(
      title = stringResource(Res.string.about_version),
      detail = appInfo.versionName ?: stringResource(Res.string.about_unknown),
      icon = Icons.Rounded.Info,
      shape = versionShape,
      modifier = Modifier.clip(versionShape)
        .semantics { stateDescription = stateLabel }
        .clickable(role = Role.Button, onClick = onToggleBuildInfo),
      trailing = {
        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = null,
          modifier = Modifier.size(20.dp).rotate(if (expanded) 90f else 270f))
      },
    )
    if (expanded) {
      AboutInfoRow(
        title = stringResource(Res.string.about_build_number),
        detail = appInfo.buildNumber ?: stringResource(Res.string.about_unknown),
        icon = Icons.Rounded.Info,
        shape = inner,
      )
    }
    // TODO: Connect update checking after release channels and platform behavior are defined.
    // The disabled button communicates availability without triggering an update check.
    AboutInfoRow(
      title = stringResource(Res.string.about_check_updates),
      detail = stringResource(Res.string.about_update_description),
      icon = Icons.Rounded.Refresh,
      shape = outer.copy(topStart = inner.topStart, topEnd = inner.topEnd),
      trailing = {
        FilledTonalButton(
          onClick = {},
          enabled = false,
          modifier = Modifier.widthIn(max = 120.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
          Text(stringResource(Res.string.about_coming_soon), textAlign = TextAlign.Center)
        }
      },
    )
  }
}

@Composable
private fun AboutInfoRow(
  title: String,
  detail: String,
  icon: ImageVector,
  shape: Shape,
  modifier: Modifier = Modifier,
  trailing: @Composable () -> Unit = {},
) {
  Surface(modifier = modifier, shape = shape, color = MaterialTheme.colorScheme.surfaceContainerLow) {
    Row(
      modifier = Modifier.heightIn(min = 80.dp).padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      trailing()
    }
  }
}
