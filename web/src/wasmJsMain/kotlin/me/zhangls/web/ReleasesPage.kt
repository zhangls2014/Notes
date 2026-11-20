package me.zhangls.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import me.zhangls.theme.layout.ContentWidth
import me.zhangls.theme.layout.LocalPaneScaffoldDirective
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo
import me.zhangls.theme.layout.contentWidth
import me.zhangls.web.resources.Res
import me.zhangls.web.resources.release_history
import me.zhangls.web.resources.release_notes
import me.zhangls.web.resources.release_published_only
import me.zhangls.web.resources.releases_empty
import me.zhangls.web.resources.releases_failure
import me.zhangls.web.resources.releases_loading
import me.zhangls.web.resources.retry

private data class ReleaseReadingAnchor(val version: String, val change: Int = -1, val fraction: Float = 0f)
private data class ParagraphPosition(val top: Float, val height: Float)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun ReleasesPage(
  result: Result<List<Release>>?,
  platform: String,
  requestedVersion: String?,
  chinese: Boolean,
  embedded: Boolean,
  onSelectVersion: (String) -> Unit,
  onRetry: () -> Unit,
) {
  val releases = remember(result, platform) {
    result?.getOrNull().orEmpty().filter { release -> release.changes.any { platform in it.platforms } }
  }
  val density = LocalDensity.current
  val windowInfo = LocalWindowAdaptiveInfo.current
  val baseDirective = LocalPaneScaffoldDirective.current
  var contentOrigin by remember { mutableStateOf(Offset.Zero) }
  var reading by remember(platform) { mutableStateOf<ReleaseReadingAnchor?>(null) }
  var restoring by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()
  val paragraphs = remember { mutableMapOf<String, ParagraphPosition>() }
  var viewportTop by remember { mutableStateOf(0f) }
  val currentVersion = reading?.version?.takeIf { version -> releases.any { it.version == version } }
    ?: requestedVersion?.takeIf { version -> releases.any { it.version == version } }
    ?: releases.firstOrNull()?.version

  BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned { contentOrigin = it.positionInWindow() }) {
    val localHinges = windowInfo.windowPosture.hingeList.filter { it.isVertical }.map {
      it.bounds.translate(-contentOrigin) / density.density
    }
    val twoPanes = releaseUsesTwoPanes(maxWidth.value, density.fontScale, localHinges)
    val inset = if (twoPanes) 32.dp else 16.dp
    val directive = baseDirective.copy(
      maxHorizontalPartitions = if (twoPanes) 2 else 1,
      maxVerticalPartitions = 1,
      defaultPanePreferredWidth = 240.dp,
      excludedBounds = if (twoPanes) baseDirective.excludedBounds else emptyList(),
      horizontalPartitionSpacerSize = if (localHinges.isEmpty()) 24.dp else 0.dp,
    )
    val strategy = rememberListDetailSceneStrategy<String>(directive = directive)

    LaunchedEffect(requestedVersion, platform, releases) {
      val saved = readReleasePosition()?.split(':')
      val savedVersion = saved?.getOrNull(0)?.takeIf { version -> releases.any { it.version == version } }
      reading = when {
        savedVersion != null && saved.getOrNull(3) == platform -> ReleaseReadingAnchor(
          savedVersion,
          saved.getOrNull(1)?.toIntOrNull() ?: -1,
          saved.getOrNull(2)?.toFloatOrNull()?.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f,
        )
        releases.any { it.version == requestedVersion } -> ReleaseReadingAnchor(requireNotNull(requestedVersion))
        else -> releases.firstOrNull()?.let { ReleaseReadingAnchor(it.version) }
      }
    }
    LaunchedEffect(reading, platform) {
      reading?.let { saveReleasePosition("${it.version}:${it.change}:${it.fraction}:$platform") }
    }
    LaunchedEffect(twoPanes, density.fontScale, requestedVersion, platform, releases, if (twoPanes) currentVersion else null) {
      if (releases.isEmpty()) return@LaunchedEffect
      val target = reading ?: ReleaseReadingAnchor(currentVersion ?: releases.first().version)
      val index = if (twoPanes) 0 else releases.indexOfFirst { it.version == target.version }.coerceAtLeast(0)
      restoring = true
      try {
        listState.scrollToItem(index)
        repeat(2) { withFrameNanos { } }
        val paragraph = paragraphs["${target.version}:${target.change}"]
        if (paragraph != null) listState.scrollToItem(index, (paragraph.top + paragraph.height * target.fraction).toInt().coerceAtLeast(0))
      } finally {
        restoring = false
      }
    }

    Column(Modifier.fillMaxSize()) {
      Column(
        Modifier.contentWidth(ContentWidth.Prose + ContentWidth.Form).padding(horizontal = inset, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        if (!embedded) Text(websiteString(Res.string.release_notes), style = MaterialTheme.typography.headlineLarge)
        FlowRow(
          Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(if (platform == "ios") "iOS" else "Android", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(websiteString(Res.string.release_published_only), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      }
      when {
        result == null -> Text(websiteString(Res.string.releases_loading), Modifier.padding(inset))
        result.isFailure -> Column(Modifier.padding(inset)) {
          Text(websiteString(Res.string.releases_failure))
          TextButton(onClick = onRetry) { Text(websiteString(Res.string.retry)) }
        }
        releases.isEmpty() -> Text(websiteString(Res.string.releases_empty), Modifier.padding(inset))
        else -> NavDisplay(
          backStack = if (twoPanes) listOf("versions", "release:$currentVersion") else listOf("versions"),
          modifier = Modifier.fillMaxSize(),
          onBack = {},
          sceneStrategies = listOf(strategy),
          entryProvider = { entry ->
            NavEntry(
              key = entry,
              metadata = if (entry == "versions") {
                ListDetailSceneStrategy.listPane() + ListDetailSceneStrategy.preferredPaneSize(width = 240.dp)
              } else {
                ListDetailSceneStrategy.detailPane()
              },
            ) {
              if (twoPanes && entry == "versions") {
                Column(
                  Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = inset, end = 12.dp, bottom = 48.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(websiteString(Res.string.release_history), Modifier.padding(12.dp),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  releases.forEach { release ->
                    TextButton(
                      onClick = { reading = ReleaseReadingAnchor(release.version); onSelectVersion(release.version) },
                      modifier = Modifier.fillMaxWidth().background(
                        if (release.version == currentVersion) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.background,
                        MaterialTheme.shapes.large,
                      ).semantics { selected = release.version == currentVersion },
                      shape = MaterialTheme.shapes.large,
                      contentPadding = PaddingValues(16.dp),
                    ) {
                      Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(release.version, style = MaterialTheme.typography.titleLarge)
                        Text(release.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
                  }
                }
              } else {
                val visible = if (twoPanes) releases.filter { it.version == currentVersion } else releases
                LazyColumn(
                  state = listState,
                  modifier = Modifier.fillMaxSize().onGloballyPositioned { viewportTop = it.positionInWindow().y },
                  contentPadding = PaddingValues(start = inset, end = inset, bottom = 48.dp),
                  verticalArrangement = Arrangement.spacedBy(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  visible.forEach { release ->
                    item(key = release.version) {
                      ReleaseCard(
                        release = release,
                        platform = platform,
                        chinese = chinese,
                        compact = !twoPanes,
                        latest = release == releases.first(),
                        modifier = Modifier.contentWidth(ContentWidth.Article).onGloballyPositioned { coordinates ->
                          val top = coordinates.positionInWindow().y
                          if (!restoring && listState.isScrollInProgress && top <= viewportTop && top + coordinates.size.height > viewportTop && reading?.version != release.version) {
                            reading = ReleaseReadingAnchor(release.version)
                          }
                        },
                        onChangePosition = { index, top, height ->
                          val itemOffset = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == release.version }?.offset
                          val cardTop = itemOffset?.let { viewportTop + it }
                          if (cardTop != null) paragraphs["${release.version}:$index"] = ParagraphPosition(top - cardTop, height)
                          if (!restoring && listState.isScrollInProgress && top <= viewportTop && top + height > viewportTop) {
                            reading = ReleaseReadingAnchor(release.version, index, (viewportTop - top) / height)
                          }
                        },
                      )
                    }
                  }
                }
              }
            }
          },
        )
      }
    }
  }
}

private operator fun androidx.compose.ui.geometry.Rect.div(value: Float) = androidx.compose.ui.geometry.Rect(
  left / value, top / value, right / value, bottom / value,
)
