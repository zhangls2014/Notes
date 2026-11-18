package me.zhangls.about

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.about.mvi.CacheManagementState
import me.zhangls.about.mvi.CacheManagementViewModel
import me.zhangls.about.ui.BrowserCacheNote
import me.zhangls.about.ui.BrowserCacheSite
import me.zhangls.about.ui.CacheFileRow
import me.zhangls.about.ui.CacheOverview
import me.zhangls.about.ui.CacheStatusCard
import me.zhangls.theme.component.AdaptiveContent
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Info
import me.zhangls.theme.icon.Storage
import me.zhangls.theme.layout.ContentWidth
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_cache_empty
import notes.feature.about.generated.resources.about_cache_empty_detail
import notes.feature.about.generated.resources.about_cache_engine
import notes.feature.about.generated.resources.about_cache_failed
import notes.feature.about.generated.resources.about_cache_failed_detail
import notes.feature.about.generated.resources.about_cache_loading
import notes.feature.about.generated.resources.about_cache_loading_detail
import notes.feature.about.generated.resources.about_cache_management
import notes.feature.about.generated.resources.about_cache_updates
import notes.feature.about.generated.resources.about_cache_web
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CacheManagementScreen(onBack: () -> Unit) {
  val direction = LocalLayoutDirection.current
  Scaffold(topBar = {
    CenteredTopAppBar(title = stringResource(Res.string.about_cache_management), navigate = onBack)
  }) { padding ->
    val viewModel = koinViewModel<CacheManagementViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val transfers by viewModel.transfers.collectAsStateWithLifecycle()
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(
        PaddingValues(
          start = padding.calculateStartPadding(direction),
          top = padding.calculateTopPadding(),
          end = padding.calculateEndPadding(direction),
        ),
      ).testTag("cache-inventory"),
      contentPadding = PaddingValues(top = 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
    ) {
      when (val current = state) {
        CacheManagementState.Loading -> item {
          CacheContent {
            CacheStatusCard(
              stringResource(Res.string.about_cache_loading),
              stringResource(Res.string.about_cache_loading_detail),
            ) { LoadingIndicator() }
          }
        }

        CacheManagementState.Failed -> item {
          CacheContent {
            CacheStatusCard(
              stringResource(Res.string.about_cache_failed),
              stringResource(Res.string.about_cache_failed_detail),
              isError = true,
            ) { Icon(Icons.Rounded.Info, contentDescription = null) }
          }
        }

        is CacheManagementState.Loaded -> cacheInventory(current.inventory, transfers.values.toList())
      }
    }
  }
}

private fun LazyListScope.cacheInventory(inventory: CacheInventory, activeTransfers: List<WebAssetTransfer>) {
  val transfers = (activeTransfers + inventory.webCheckpoints)
    .distinctBy { it.file.site to it.file.name }
    .sortedBy { it.file.name }
  item { CacheContent { CacheOverview(inventory.knownBytes) } }
  cacheFiles(Res.string.about_cache_web, inventory.webFiles, transfers)
  cacheFiles(Res.string.about_cache_updates, inventory.updateFiles)
  if (transfers.isEmpty() && inventory.webFiles.isEmpty() && inventory.updateFiles.isEmpty()) {
    item {
      CacheContent(Modifier.padding(top = 20.dp)) {
        CacheStatusCard(
          stringResource(Res.string.about_cache_empty),
          stringResource(Res.string.about_cache_empty_detail),
        ) { Icon(Icons.Rounded.Storage, contentDescription = null) }
      }
    }
  }
  item { CacheHeading(Res.string.about_cache_engine) }
  val sites = inventory.webEngineSites
  item { CacheContent { BrowserCacheNote(sites) } }
  if (!sites.isNullOrEmpty()) {
    itemsIndexed(sites) { index, site ->
      CacheContent(Modifier.padding(top = if (index == 0) 8.dp else 2.dp)) {
        BrowserCacheSite(site, index, sites.size)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.cacheFiles(
  title: StringResource,
  files: List<CachedFile>,
  transfers: List<WebAssetTransfer> = emptyList(),
) {
  val completed = files.filterNot { file ->
    transfers.any { it.file.site == file.site && it.file.name == file.name }
  }
  val count = completed.size + transfers.size
  if (count == 0) return
  item { CacheHeading(title) }
  itemsIndexed(transfers) { index, transfer ->
    CacheContent(Modifier.padding(top = if (index == 0) 0.dp else ListItemDefaults.SegmentedGap)) {
      CacheFileRow(transfer.file, index, count, transfer)
    }
  }
  itemsIndexed(completed) { index, file ->
    val position = index + transfers.size
    CacheContent(Modifier.padding(top = if (position == 0) 0.dp else ListItemDefaults.SegmentedGap)) {
      CacheFileRow(file, position, count)
    }
  }
}

@Composable
private fun CacheContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  AdaptiveContent(maxWidth = ContentWidth.Form) {
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp)) { content() }
  }
}

@Composable
private fun CacheHeading(title: StringResource) {
  CacheContent {
    Text(
      stringResource(title),
      Modifier.padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 8.dp).semantics { heading() },
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
    )
  }
}
