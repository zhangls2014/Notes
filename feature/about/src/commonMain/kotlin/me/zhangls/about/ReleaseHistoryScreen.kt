package me.zhangls.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import me.zhangls.theme.component.AdaptiveContent
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.layout.ContentWidth
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo
import me.zhangls.theme.layout.isExpandedWidthOrWider
import me.zhangls.theme.layout.isExtraLargeWidthOrWider
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_release_added
import notes.feature.about.generated.resources.about_release_empty
import notes.feature.about.generated.resources.about_release_failed
import notes.feature.about.generated.resources.about_release_fixed
import notes.feature.about.generated.resources.about_release_history
import notes.feature.about.generated.resources.about_release_improved
import notes.feature.about.generated.resources.about_release_loading
import notes.feature.about.generated.resources.about_release_unreleased
import org.jetbrains.compose.resources.stringResource

private const val EXTRA_LARGE_COLUMNS = 3

@Composable
internal fun ReleaseHistoryScreen(onBack: () -> Unit) {
  val result by rememberReleaseHistory()
  val window = LocalWindowAdaptiveInfo.current
  val columns = when {
    window.isExtraLargeWidthOrWider -> EXTRA_LARGE_COLUMNS
    window.isExpandedWidthOrWider -> 2
    else -> 1
  }
  val spacing = 16.dp
  val maxWidth = ContentWidth.Form * columns + spacing * (columns - 1)
  Scaffold(topBar = {
    CenteredTopAppBar(title = stringResource(Res.string.about_release_history), navigate = onBack)
  }) { padding ->
    AdaptiveContent(modifier = Modifier.fillMaxSize().padding(padding), maxWidth = maxWidth) {
      LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize().testTag("release-history-grid"),
        contentPadding = PaddingValues(spacing),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalItemSpacing = spacing,
      ) {
        val loaded = result
        when {
          loaded == null -> item(span = StaggeredGridItemSpan.FullLine) {
            Text(stringResource(Res.string.about_release_loading))
          }

          loaded.isFailure -> item(span = StaggeredGridItemSpan.FullLine) {
            Text(stringResource(Res.string.about_release_failed))
          }

          loaded.getOrThrow().isEmpty() -> item(span = StaggeredGridItemSpan.FullLine) {
            Text(stringResource(Res.string.about_release_empty))
          }

          else -> items(loaded.getOrThrow(), key = { it.version }) { release ->
            ReleaseHistoryItem(release)
          }
        }
      }
    }
  }
}

@Composable
private fun ReleaseHistoryItem(release: AppRelease) {
  Surface(
    modifier = Modifier.testTag("release-${release.version}"),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(release.version, style = MaterialTheme.typography.titleMedium)
      Text(release.date ?: stringResource(Res.string.about_release_unreleased))
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
}
