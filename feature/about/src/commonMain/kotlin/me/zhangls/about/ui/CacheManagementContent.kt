package me.zhangls.about.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import me.zhangls.about.CachedFile
import me.zhangls.about.WebAssetTransfer
import me.zhangls.theme.icon.Code
import me.zhangls.theme.icon.Download
import me.zhangls.theme.icon.FormatSize
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Info
import me.zhangls.theme.icon.Language
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_cache_engine_empty
import notes.feature.about.generated.resources.about_cache_downloading
import notes.feature.about.generated.resources.about_cache_verifying
import notes.feature.about.generated.resources.about_cache_waiting_to_resume
import notes.feature.about.generated.resources.about_cache_engine_note
import notes.feature.about.generated.resources.about_cache_engine_unavailable
import notes.feature.about.generated.resources.about_cache_font_name
import notes.feature.about.generated.resources.about_cache_skiko_name
import notes.feature.about.generated.resources.about_cache_total
import notes.feature.about.generated.resources.about_cache_total_detail
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToLong

@Composable
internal fun CacheOverview(bytes: Long) {
  val colors = MaterialTheme.colorScheme
  val formatted = formatCacheSize(bytes)
  val unitStyle = MaterialTheme.typography.titleLarge.toSpanStyle()
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = colors.primaryContainer,
    contentColor = colors.onPrimaryContainer,
  ) {
    Column(Modifier.padding(20.dp)) {
      Text(stringResource(Res.string.about_cache_total), style = MaterialTheme.typography.labelLarge)
      Text(
        buildAnnotatedString {
          append(formatted.substringBeforeLast(' '))
          append(' ')
          withStyle(unitStyle) { append(formatted.substringAfterLast(' ')) }
        },
        style = MaterialTheme.typography.displaySmall,
      )
      Text(stringResource(Res.string.about_cache_total_detail), style = MaterialTheme.typography.bodySmall)
    }
  }
}

@Composable
internal fun CacheFileRow(file: CachedFile, index: Int, count: Int, transfer: WebAssetTransfer? = null) {
  val colors = MaterialTheme.colorScheme
  val (name, icon) = when {
    file.site == null -> file.name to Icons.Rounded.Download
    file.name.endsWith(".wasm") -> stringResource(Res.string.about_cache_skiko_name) to Icons.Rounded.Code
    else -> stringResource(Res.string.about_cache_font_name) to Icons.Rounded.FormatSize
  }
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = cacheSegmentShape(index, count),
    color = colors.surfaceContainer,
    contentColor = colors.onSurface,
  ) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(14.dp)) {
      // Reserve enough reading width for filenames before placing size in a separate column.
      val inlineSize = transfer == null && maxWidth / LocalDensity.current.fontScale >= 300.dp
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = if (file.site == null) Alignment.CenterVertically else Alignment.Top,
      ) {
        CacheIcon(icon, colors.surface, colors.primary)
        Column(Modifier.weight(1f)) {
          Text(name, style = MaterialTheme.typography.titleMedium)
          Text(file.relativePath, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
          if (transfer != null) {
            CacheDownloadProgress(transfer)
          } else if (!inlineSize) {
            CacheFileSize(file.bytes, Modifier.padding(top = 6.dp))
          }
        }
        if (inlineSize) CacheFileSize(file.bytes)
      }
    }
  }
}

@Composable
private fun CacheDownloadProgress(transfer: WebAssetTransfer) {
  val receivedBytes = transfer.receivedBytes
  val totalBytes = transfer.file.bytes
  val fraction = receivedBytes.toFloat() / totalBytes
  val percent = (receivedBytes * 100 / totalBytes).toInt()
  val label = when {
    !transfer.active -> stringResource(Res.string.about_cache_waiting_to_resume, "$percent%")
    receivedBytes == totalBytes -> stringResource(Res.string.about_cache_verifying)
    else -> stringResource(Res.string.about_cache_downloading, "$percent%")
  }
  Text(label, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelLarge)
  LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
}

@Composable
private fun CacheFileSize(bytes: Long, modifier: Modifier = Modifier) {
  Text(formatCacheSize(bytes), modifier, style = MaterialTheme.typography.labelLarge)
}

@Composable
internal fun BrowserCacheNote(sites: List<String>?) {
  val colors = MaterialTheme.colorScheme
  val message = when {
    sites == null -> Res.string.about_cache_engine_unavailable
    sites.isEmpty() -> Res.string.about_cache_engine_empty
    else -> Res.string.about_cache_engine_note
  }
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color = colors.surfaceContainerHigh,
    contentColor = colors.onSurfaceVariant,
  ) {
    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(18.dp))
      Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
internal fun BrowserCacheSite(site: String, index: Int, count: Int) {
  val colors = MaterialTheme.colorScheme
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = cacheSegmentShape(index, count),
    color = colors.surfaceContainer,
    contentColor = colors.onSurface,
  ) {
    Row(
      Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CacheIcon(Icons.Rounded.Language, colors.surface, colors.primary)
      Text(site, style = MaterialTheme.typography.titleMedium)
    }
  }
}

@Composable
internal fun CacheStatusCard(
  title: String,
  detail: String,
  isError: Boolean = false,
  marker: @Composable () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    shape = MaterialTheme.shapes.extraLarge,
    color = if (isError) colors.errorContainer else colors.surfaceContainer,
    contentColor = if (isError) colors.onErrorContainer else colors.onSurface,
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      marker()
      Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
      Text(detail, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun cacheSegmentShape(index: Int, count: Int): Shape =
  if (count == 1) MaterialTheme.shapes.large else ListItemDefaults.segmentedShapes(index, count).shape

@Composable
private fun CacheIcon(icon: ImageVector, color: Color, contentColor: Color) {
  Surface(Modifier.size(34.dp), shape = MaterialTheme.shapes.medium, color = color, contentColor = contentColor) {
    Box(contentAlignment = Alignment.Center) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    }
  }
}

private fun formatCacheSize(bytes: Long): String {
  if (bytes < 1_000) return "$bytes B"
  val (unit, divisor) = when {
    bytes >= 1_000_000_000L -> "GB" to 1_000_000_000L
    bytes >= 1_000_000L -> "MB" to 1_000_000L
    else -> "KB" to 1_000L
  }
  val tenths = (bytes.toDouble() / divisor * 10).roundToLong()
  return "${tenths / 10}.${tenths % 10} $unit"
}
