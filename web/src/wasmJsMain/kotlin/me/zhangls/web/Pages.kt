package me.zhangls.web

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.StarFill
import me.zhangls.theme.icon.ViewColumn
import me.zhangls.web.resources.Res
import me.zhangls.web.resources.adaptive_layout
import me.zhangls.web.resources.adaptive_layout_description
import me.zhangls.web.resources.android_global
import me.zhangls.web.resources.android_mainland
import me.zhangls.web.resources.download
import me.zhangls.web.resources.download_android
import me.zhangls.web.resources.downloads
import me.zhangls.web.resources.downloads_empty
import me.zhangls.web.resources.favorites_search
import me.zhangls.web.resources.favorites_search_description
import me.zhangls.web.resources.home_description
import me.zhangls.web.resources.home_pitch
import me.zhangls.web.resources.home_pitch_wide
import me.zhangls.web.resources.mail_preview
import me.zhangls.web.resources.mail_preview_description
import me.zhangls.web.resources.release_added
import me.zhangls.web.resources.release_fixed
import me.zhangls.web.resources.release_improved
import me.zhangls.web.resources.view_release_notes
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun HomePage(
  wide: Boolean,
  compactHeading: Boolean,
  onDownload: () -> Unit,
  onReleases: () -> Unit,
) {
  val pitch: @Composable (Modifier) -> Unit = { modifier ->
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
      Text(
        websiteString(if (wide) Res.string.home_pitch_wide else Res.string.home_pitch),
        style = when {
          wide -> MaterialTheme.typography.displayLarge
          compactHeading -> MaterialTheme.typography.headlineSmall
          else -> MaterialTheme.typography.headlineLarge
        },
        fontWeight = FontWeight.Bold,
      )
      Text(
        websiteString(Res.string.home_description),
        style = MaterialTheme.typography.bodyLarge,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onDownload) { Text(websiteString(Res.string.download_android)) }
        TextButton(onClick = onReleases) { Text(websiteString(Res.string.view_release_notes)) }
      }
    }
  }
  val preview: @Composable (Modifier) -> Unit = { modifier ->
    Surface(modifier, shape = RoundedCornerShape(48.dp, 64.dp, 36.dp, 64.dp),
      color = MaterialTheme.colorScheme.primaryContainer) {
      Box(Modifier.fillMaxWidth().padding(20.dp)) {
        Icon(
          Icons.Rounded.StarFill,
          contentDescription = null,
          modifier = Modifier.align(Alignment.TopEnd).size(100.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
        Image(
          painterResource(Res.drawable.mail_preview),
          websiteString(Res.string.mail_preview_description),
          Modifier.fillMaxWidth().padding(top = 30.dp).clip(MaterialTheme.shapes.large),
        )
      }
    }
  }
  if (wide) {
    Row(
      Modifier.fillMaxWidth().padding(vertical = 32.dp),
      horizontalArrangement = Arrangement.spacedBy(24.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      pitch(Modifier.weight(2f))
      preview(Modifier.weight(3f))
    }
  } else {
    pitch(Modifier.fillMaxWidth())
    preview(Modifier.fillMaxWidth())
  }
  val features: @Composable (Modifier, Modifier) -> Unit = { first, second ->
    Surface(first, color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.extraLarge) {
      Feature(
        Icons.Rounded.StarFill,
        websiteString(Res.string.favorites_search),
        websiteString(Res.string.favorites_search_description),
      )
    }
    Surface(second, color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.extraLarge) {
      Feature(
        Icons.Rounded.ViewColumn,
        websiteString(Res.string.adaptive_layout),
        websiteString(Res.string.adaptive_layout_description),
      )
    }
  }
  if (wide) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      features(Modifier.weight(5f), Modifier.weight(4f))
    }
  } else {
    features(Modifier.fillMaxWidth(), Modifier.fillMaxWidth())
  }
}

@Composable
private fun Feature(icon: ImageVector, title: String, description: String) {
  Row(Modifier.padding(24.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      Text(description, style = MaterialTheme.typography.bodyLarge)
    }
  }
}

@Composable
internal fun DownloadPage(builds: List<PublishedBuild>, compactHeading: Boolean) {
  Text(
    websiteString(Res.string.downloads),
    style = if (compactHeading) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge,
  )
  val current = builds.associateBy { it.distributionChannel }.values.mapNotNull { build ->
    build.artifactUrl?.let { build to it }
  }
  if (current.isEmpty()) {
    Text(websiteString(Res.string.downloads_empty))
  }
  current.forEach { (build, link) ->
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.surfaceContainerLow) {
      Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val channelLabel = when (build.distributionChannel) {
          "android-prd-global" -> websiteString(Res.string.android_global)
          "android-prd-mainland" -> websiteString(Res.string.android_mainland)
          else -> "iOS"
        }
        Text(channelLabel, style = MaterialTheme.typography.titleLarge)
        Text(build.version)
        Button(onClick = { openPage(link) }) {
          Text(websiteString(Res.string.download))
        }
      }
    }
  }
}

@Composable
internal fun ReleaseCard(
  release: Release,
  platform: String,
  chinese: Boolean,
  onOpenRelease: (() -> Unit)? = null,
) {
  Surface(
    Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      if (onOpenRelease == null) {
        Text(release.version, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
      } else {
        TextButton(onClick = onOpenRelease) {
          Text(release.version, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
      }
      Text(release.date, color = MaterialTheme.colorScheme.onSurfaceVariant)
      val headings = listOf(
        websiteString(Res.string.release_added),
        websiteString(Res.string.release_improved),
        websiteString(Res.string.release_fixed),
      )
      listOf("added", "improved", "fixed").zip(headings).forEach { (type, label) ->
        val changes = release.changes.filter { it.type == type && platform in it.platforms }
        if (changes.isNotEmpty()) {
          ReleaseHeading(label)
          changes.forEach { change ->
            Text("• " + change.text.getValue(if (chinese) "zh" else "en"), style = MaterialTheme.typography.bodyLarge)
          }
        }
      }
    }
  }
}

@Composable
private fun ReleaseHeading(label: String) {
  Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
    Text(label, Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelLarge)
  }
}
