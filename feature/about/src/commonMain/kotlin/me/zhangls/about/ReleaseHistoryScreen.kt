package me.zhangls.about

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import me.zhangls.about.ui.ReleaseChanges
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.ViewAgenda
import me.zhangls.theme.icon.ViewColumn
import me.zhangls.theme.layout.LocalPaneScaffoldDirective
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo
import me.zhangls.theme.layout.isExpandedWidthOrWider
import me.zhangls.theme.layout.isExtraLargeWidthOrWider
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_release_empty
import notes.feature.about.generated.resources.about_release_failed
import notes.feature.about.generated.resources.about_release_history
import notes.feature.about.generated.resources.about_release_loading
import notes.feature.about.generated.resources.about_release_switch_columns
import notes.feature.about.generated.resources.about_release_switch_single
import notes.feature.about.generated.resources.about_release_unreleased
import org.jetbrains.compose.resources.stringResource

private const val EXTRA_LARGE_COLUMNS = 3
private const val LAYOUT_ICON_ANIMATION_MILLIS = 200

// Both layout glyphs retain their visual orientation after a half turn.
private const val LAYOUT_ICON_ROTATION_DEGREES = 180f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ReleaseHistoryScreen(onBack: () -> Unit) {
  val result by rememberReleaseHistory()
  val window = LocalWindowAdaptiveInfo.current
  val fullyOpen = window.windowPosture.isFullyOpen
  val canToggle = fullyOpen && window.windowPosture.hingeList.any { it.isVertical }
  var manualColumns by rememberSaveable(fullyOpen) { mutableStateOf(false) }
  val manualSplit = canToggle && manualColumns
  val columns = when {
    fullyOpen -> 1
    window.isExtraLargeWidthOrWider -> EXTRA_LARGE_COLUMNS
    window.isExpandedWidthOrWider -> 2
    else -> 1
  }
  Scaffold(topBar = {
    CenteredTopAppBar(
      title = stringResource(Res.string.about_release_history),
      navigate = onBack,
      actions = {
        if (canToggle) {
          val rotation = animateFloatAsState(
            targetValue = if (manualSplit) LAYOUT_ICON_ROTATION_DEGREES else 0f,
            animationSpec = tween(LAYOUT_ICON_ANIMATION_MILLIS, easing = FastOutSlowInEasing),
            label = "Release history layout rotation",
          )
          IconButton(onClick = { manualColumns = !manualColumns }, shapes = IconButtonDefaults.shapes()) {
            val label = if (manualSplit) {
              Res.string.about_release_switch_single
            } else {
              Res.string.about_release_switch_columns
            }
            Icon(
              imageVector = if (manualSplit) Icons.Rounded.ViewAgenda else Icons.Rounded.ViewColumn,
              contentDescription = stringResource(label),
              modifier = Modifier.graphicsLayer { rotationZ = rotation.value },
            )
          }
        }
      },
    )
  }) { padding ->
    ReleaseHistoryGrid(result, columns, manualSplit, Modifier.padding(padding))
  }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ReleaseHistoryGrid(
  result: Result<List<AppRelease>>?,
  columns: Int,
  manualSplit: Boolean,
  modifier: Modifier = Modifier,
) {
  val spacing = 16.dp
  var viewport by remember { mutableStateOf(Rect.Zero) }
  val excluded = if (manualSplit) LocalPaneScaffoldDirective.current.excludedBounds else emptyList()
  val direction = LocalLayoutDirection.current
  val hingeColumns = if (manualSplit) {
    remember(viewport, excluded, direction) {
      ReleaseHistoryHingeColumns(viewport, excluded, spacing, spacing, direction)
    }
  } else {
    null
  }
  Box(
    modifier = modifier.fillMaxSize().onGloballyPositioned {
      // Scaffold places this content directly in a full-screen page. Ancestor graphics layers
      // affect window bounds, but must not feed the navigation drawing transform back into layout.
      val origin = it.findRootCoordinates().positionInWindow() + it.positionInParent()
      viewport = Rect(origin, it.size.toSize())
    },
  ) {
    if (!manualSplit || viewport.width > 0f) {
      LazyVerticalStaggeredGrid(
        columns = hingeColumns ?: StaggeredGridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize().testTag("release-history-grid"),
        contentPadding = PaddingValues(spacing),
        horizontalArrangement = hingeColumns ?: Arrangement.spacedBy(spacing),
        verticalItemSpacing = spacing,
      ) {
        releaseHistoryEntries(result, if (manualSplit) null else StaggeredGridItemSpan.FullLine)
      }
    }
  }
}

private fun LazyStaggeredGridScope.releaseHistoryEntries(
  result: Result<List<AppRelease>>?,
  statusSpan: StaggeredGridItemSpan?,
) {
  when {
    result == null -> item(span = statusSpan) { Text(stringResource(Res.string.about_release_loading)) }
    result.isFailure -> item(span = statusSpan) { Text(stringResource(Res.string.about_release_failed)) }
    result.getOrThrow().isEmpty() -> item(span = statusSpan) { Text(stringResource(Res.string.about_release_empty)) }
    else -> items(result.getOrThrow(), key = { it.version }) { ReleaseHistoryItem(it) }
  }
}

@Composable
private fun ReleaseHistoryItem(release: AppRelease) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag("release-${release.version}"),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(release.version, style = MaterialTheme.typography.titleMedium)
      Text(release.date ?: stringResource(Res.string.about_release_unreleased))
      ReleaseChanges(release)
    }
  }
}
