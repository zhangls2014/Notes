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
import me.zhangls.web.resources.mail_preview
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun HomePage(
  chinese: Boolean,
  wide: Boolean,
  compactHeading: Boolean,
  onDownload: () -> Unit,
  onReleases: () -> Unit,
) {
  val pitch: @Composable (Modifier) -> Unit = { modifier ->
    Column(modifier, verticalArrangement = Arrangement.spacedBy(20.dp)) {
      Text(
        when {
          chinese -> "随手收藏\n重要邮件"
          wide -> "Keep important\nmail close"
          else -> "Keep important mail close"
        },
        style = when {
          wide -> MaterialTheme.typography.displayLarge
          compactHeading -> MaterialTheme.typography.headlineSmall
          else -> MaterialTheme.typography.headlineLarge
        },
        fontWeight = FontWeight.Bold,
      )
      Text(
        if (chinese) "适用于 Android 和 iOS 的邮件管理示例应用"
        else "An email management sample app for Android and iOS.",
        style = MaterialTheme.typography.bodyLarge,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onDownload) { Text(if (chinese) "下载 Android" else "Download Android") }
        TextButton(onClick = onReleases) { Text(if (chinese) "查看更新日志" else "Release notes") }
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
          if (chinese) "摘星的邮件列表和详情并排显示" else "Notes with its email list and detail side by side",
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
        if (chinese) "收藏与搜索" else "Favorites and search",
        if (chinese) "为重要邮件加星，并通过搜索快速找到。" else "Star important mail and find it quickly with search.",
      )
    }
    Surface(second, color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.extraLarge) {
      Feature(
        Icons.Rounded.ViewColumn,
        if (chinese) "自适应布局" else "Adaptive layout",
        if (chinese) "在不同窗口尺寸下浏览邮件列表与详情。" else "Browse lists and details across window sizes.",
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
internal fun DownloadPage(chinese: Boolean, builds: List<PublishedBuild>, compactHeading: Boolean) {
  Text(
    if (chinese) "下载" else "Downloads",
    style = if (compactHeading) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineLarge,
  )
  val current = builds.associateBy { it.distributionChannel }.values.mapNotNull { build ->
    build.download?.let { build to it }
  }
  if (current.isEmpty()) {
    Text(if (chinese) "暂无已发布构建。" else "No published builds yet.")
  }
  current.forEach { (build, link) ->
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
      color = MaterialTheme.colorScheme.surfaceContainerLow) {
      Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(build.label(chinese), style = MaterialTheme.typography.titleLarge)
        Text(build.version)
        Button(onClick = { openPage(link) }) {
          Text(if (chinese) "下载" else "Download")
        }
      }
    }
  }
}

@Composable
internal fun NoticesPage(
  chinese: Boolean,
  platform: String,
  onOpen: (String) -> Unit,
) {
  Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerLow) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(if (platform == "ios") "iOS" else "Android", style = MaterialTheme.typography.headlineMedium)
      TextButton(onClick = { onOpen("/open-source/$platform.html") }) {
        Text(if (chinese) "查看开源声明" else "View open-source notices")
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
      val headings = if (chinese) listOf("新增", "优化", "修复") else listOf("Added", "Improved", "Fixed")
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
