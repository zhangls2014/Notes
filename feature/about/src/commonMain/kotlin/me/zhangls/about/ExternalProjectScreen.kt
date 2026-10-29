package me.zhangls.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.zhangls.framework.toast.ToastGlobalNotifier
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_project_load_failed
import notes.feature.about.generated.resources.about_project_open_browser
import notes.feature.about.generated.resources.about_project_source
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
internal fun ExternalProjectScreen(url: String, onBack: () -> Unit) {
  var title by remember(url) { mutableStateOf<String?>(null) }
  var loading by remember(url) { mutableStateOf(true) }
  var failed by remember(url) { mutableStateOf(false) }
  val uriHandler = LocalUriHandler.current
  val notifier = koinInject<ToastGlobalNotifier>()
  Scaffold(topBar = {
    NoticesTopAppBar(
      title = title ?: stringResource(Res.string.about_project_source),
      onBack = onBack,
      isDetail = true,
    )
  }) { padding ->
    Column(Modifier.fillMaxSize().padding(padding.webViewContentPadding(LocalLayoutDirection.current))) {
      TextButton(onClick = {
        try {
          uriHandler.openUri(url)
        } catch (_: IllegalArgumentException) {
          notifier.showToast(Res.string.about_project_load_failed)
        }
      }) { Text(stringResource(Res.string.about_project_open_browser)) }
      if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
      if (failed) Text(stringResource(Res.string.about_project_load_failed))
      ExternalProjectWebView(
        url = url,
        onLoading = {
          loading = it
          if (it) failed = false
        },
        onTitle = { title = it?.takeIf(String::isNotBlank) },
        onError = {
          loading = false
          failed = true
        },
        bottomInset = padding.calculateBottomPadding(),
        modifier = Modifier.fillMaxWidth().weight(1f),
      )
    }
  }
}

@Composable
internal expect fun ExternalProjectWebView(
  url: String,
  onLoading: (Boolean) -> Unit,
  onError: () -> Unit,
  onTitle: (String?) -> Unit,
  modifier: Modifier = Modifier,
  bottomInset: Dp = 0.dp,
)
