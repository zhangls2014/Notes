package me.zhangls.about.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import me.zhangls.about.cacheFileAbsolutePath
import me.zhangls.about.CachedFile
import me.zhangls.about.WebAssetTransfer
import me.zhangls.about.WebAssetKind
import me.zhangls.theme.icon.Code
import me.zhangls.theme.icon.Download
import me.zhangls.theme.icon.FormatSize
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Info
import me.zhangls.theme.icon.Language
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_cache_clear_resource
import notes.feature.about.generated.resources.about_cache_engine_empty
import notes.feature.about.generated.resources.about_cache_downloading
import notes.feature.about.generated.resources.about_cache_verifying
import notes.feature.about.generated.resources.about_cache_waiting_to_resume
import notes.feature.about.generated.resources.about_cache_engine_note
import notes.feature.about.generated.resources.about_cache_engine_unavailable
import notes.feature.about.generated.resources.about_cache_font_name
import notes.feature.about.generated.resources.about_cache_skiko_name
import notes.feature.about.generated.resources.about_cache_wasm_name
import notes.feature.about.generated.resources.about_cache_unknown_font_name
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
internal fun CacheFileRow(
  file: CachedFile,
  index: Int,
  count: Int,
  transfer: WebAssetTransfer?,
  onClear: ((CachedFile) -> Unit)?,
  activeSwipe: String?,
  onSwipe: (String) -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val delete = onClear?.takeUnless { transfer?.active == true }
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("cache-row/${file.relativePath}"),
    shape = cacheSegmentShape(index, count),
    color = colors.surfaceContainer,
    contentColor = colors.onSurface,
  ) {
    val content: @Composable () -> Unit = {
      Box(Modifier.fillMaxWidth().padding(14.dp)) {
        CacheFileDetails(file, transfer)
      }
    }
    if (delete == null) {
      content()
    } else {
      CacheFileSwipeActions(
        isActive = activeSwipe == file.relativePath,
        onSwipe = { onSwipe(file.relativePath) },
        label = stringResource(Res.string.about_cache_clear_resource, file.relativePath),
        onDelete = { delete(file) },
        content = content,
      )
    }
  }
}

@Composable
private fun CacheFileDetails(file: CachedFile, transfer: WebAssetTransfer?) {
  val colors = MaterialTheme.colorScheme
  val (name, icon) = when {
    file.site == null -> file.name to Icons.Rounded.Download
    file.kind == WebAssetKind.Skiko -> stringResource(Res.string.about_cache_skiko_name) to Icons.Rounded.Code
    file.kind == WebAssetKind.MiSans -> stringResource(Res.string.about_cache_font_name) to Icons.Rounded.FormatSize
    file.name.endsWith(".wasm") -> stringResource(Res.string.about_cache_wasm_name) to Icons.Rounded.Code
    else -> stringResource(Res.string.about_cache_unknown_font_name) to Icons.Rounded.FormatSize
  }
  Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = if (file.site == null) Alignment.CenterVertically else Alignment.Top,
  ) {
    CacheIcon(icon, colors.surface, colors.primary, Modifier.align(Alignment.CenterVertically))
    Column(Modifier.weight(1f)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        CacheFileSize(file.bytes)
      }
      Text(
        cacheFileAbsolutePath(file.relativePath),
        Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
      )
      if (transfer != null) CacheDownloadProgress(transfer)
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
    modifier = Modifier.fillMaxWidth().testTag("cache-browser-note"),
    shape = MaterialTheme.shapes.large,
    color = colors.surfaceContainerHigh,
    contentColor = colors.onSurfaceVariant,
  ) {
    Row(
      Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CacheIcon(Icons.Rounded.Info, colors.surface, colors.primary)
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
private fun CacheIcon(icon: ImageVector, color: Color, contentColor: Color, modifier: Modifier = Modifier) {
  Surface(
    modifier.size(34.dp).testTag("cache-item-icon"),
    shape = MaterialTheme.shapes.medium,
    color = color,
    contentColor = contentColor,
  ) {
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
