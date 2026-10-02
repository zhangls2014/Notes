package me.zhangls.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.about.mvi.AboutIntent
import me.zhangls.about.mvi.AboutViewModel
import me.zhangls.about.ui.AboutBrandCard
import me.zhangls.about.ui.AboutVersionGroup
import me.zhangls.theme.component.AdaptiveContent
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.layout.ContentWidth
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_title
import notes.feature.about.generated.resources.about_version_information
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun AboutScreen(appInfo: AboutAppInfo, onBackPressed: () -> Unit) {
  val viewModel = koinViewModel<AboutViewModel> { parametersOf(appInfo) }
  val state by viewModel.state.collectAsStateWithLifecycle()
  Scaffold(topBar = {
    CenteredTopAppBar(title = stringResource(Res.string.about_title), navigate = onBackPressed)
  }) { padding ->
    AdaptiveContent(maxWidth = ContentWidth.Form) {
      Column(
        modifier = Modifier.fillMaxSize().padding(padding)
          .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        AboutBrandCard()
        Text(
          text = stringResource(Res.string.about_version_information),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(start = 16.dp, top = 8.dp),
        )
        AboutVersionGroup(
          appInfo = state.appInfo,
          expanded = state.isBuildInfoExpanded,
          onToggleBuildInfo = { viewModel.sendIntent(AboutIntent.ToggleBuildInfo) },
        )
      }
    }
  }
}
