package me.zhangls.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.about.mvi.AboutIntent
import me.zhangls.about.mvi.AboutViewModel
import me.zhangls.about.mvi.UpdateStatus
import me.zhangls.about.ui.AboutBrandCard
import me.zhangls.about.ui.AboutInfoRow
import me.zhangls.about.ui.AboutVersionGroup
import me.zhangls.theme.component.AdaptiveContent
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.icon.Code
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Language
import me.zhangls.theme.icon.Storage
import me.zhangls.theme.layout.ContentWidth
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_cache_description
import notes.feature.about.generated.resources.about_cache_management
import notes.feature.about.generated.resources.about_notices_description
import notes.feature.about.generated.resources.about_storage
import notes.feature.about.generated.resources.about_web_and_notices
import notes.feature.about.generated.resources.about_open_source_notices
import notes.feature.about.generated.resources.about_title
import notes.feature.about.generated.resources.about_version_information
import notes.feature.about.generated.resources.about_website
import notes.feature.about.generated.resources.about_website_description
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AboutScreen(
  onBack: () -> Unit,
  onOpenSource: () -> Unit,
  onOpenReleaseHistory: () -> Unit,
  onOpenWebsite: () -> Unit,
  onOpenCacheManagement: () -> Unit,
) {
  val viewModel = koinViewModel<AboutViewModel>()
  val state by viewModel.state.collectAsStateWithLifecycle()
  val releases by viewModel.releases.collectAsStateWithLifecycle()
  val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()
  val checkForUpdates = { viewModel.sendIntent(AboutIntent.CheckForUpdates) }
  val checking = updateStatus == UpdateStatus.Checking
  UpdateCheckDialog(
    updateStatus,
    onDismiss = { viewModel.sendIntent(AboutIntent.DismissUpdateResult) },
    onRetry = checkForUpdates,
    onUpdateAction = { viewModel.sendIntent(AboutIntent.UpdateAction) },
  )
  val layoutDirection = LocalLayoutDirection.current
  Scaffold(topBar = {
    CenteredTopAppBar(title = stringResource(Res.string.about_title), navigate = onBack)
  }) { padding ->
    AdaptiveContent(
      modifier = Modifier.fillMaxSize().padding(
        PaddingValues(
          start = padding.calculateStartPadding(layoutDirection),
          top = padding.calculateTopPadding(),
          end = padding.calculateEndPadding(layoutDirection),
        ),
      ).verticalScroll(rememberScrollState()),
      maxWidth = ContentWidth.Form,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = padding.calculateBottomPadding()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        AboutBrandCard()
        AboutSectionTitle(stringResource(Res.string.about_version_information))
        AboutVersionGroup(
          appInfo = state.appInfo,
          releases = releases,
          expanded = state.isVersionInfoExpanded,
          onToggleVersionInfo = { viewModel.sendIntent(AboutIntent.ToggleVersionInfo) },
          onOpenReleaseHistory = onOpenReleaseHistory,
          canCheckForUpdates = viewModel.canCheckForUpdates,
          checking = checking,
          onCheckForUpdates = checkForUpdates,
        )
        AboutSectionTitle(stringResource(Res.string.about_web_and_notices))
        AboutWebsiteGroup(onOpenWebsite, onOpenSource)
        AboutSectionTitle(stringResource(Res.string.about_storage))
        val shape = MaterialTheme.shapes.extraLarge
        AboutInfoRow(
          title = stringResource(Res.string.about_cache_management),
          detail = stringResource(Res.string.about_cache_description),
          icon = Icons.Rounded.Storage,
          shape = shape,
          modifier = Modifier.clip(shape).clickable(role = Role.Button, onClick = onOpenCacheManagement),
        )
      }
    }
  }
}

@Composable
private fun AboutSectionTitle(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 16.dp, top = 8.dp).semantics { heading() },
  )
}

@Composable
private fun AboutWebsiteGroup(onOpenWebsite: () -> Unit, onOpenSource: () -> Unit) {
  val outer = MaterialTheme.shapes.extraLarge
  val inner = MaterialTheme.shapes.extraSmall
  val websiteShape = outer.copy(bottomStart = inner.bottomStart, bottomEnd = inner.bottomEnd)
  val noticesShape = outer.copy(topStart = inner.topStart, topEnd = inner.topEnd)
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    AboutInfoRow(
      title = stringResource(Res.string.about_website),
      detail = stringResource(Res.string.about_website_description),
      icon = Icons.Rounded.Language,
      shape = websiteShape,
      modifier = Modifier.clip(websiteShape).clickable(role = Role.Button, onClick = onOpenWebsite),
    )
    AboutInfoRow(
      title = stringResource(Res.string.about_open_source_notices),
      detail = stringResource(Res.string.about_notices_description),
      icon = Icons.Rounded.Code,
      shape = noticesShape,
      modifier = Modifier.clip(noticesShape).clickable(role = Role.Button, onClick = onOpenSource),
    )
  }
}
