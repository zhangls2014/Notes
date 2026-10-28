package me.zhangls.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.about.api.OpenSourcePage
import me.zhangls.framework.toast.ToastGlobalNotifier
import me.zhangls.theme.component.CenteredTopAppBar
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_notices_open_failed
import notes.feature.about.generated.resources.about_open_source_notices
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
internal fun OpenSourceNoticesScreen(
  page: OpenSourcePage,
  anchor: String?,
  appInfo: AboutAppInfo,
  onBack: () -> Unit,
  onNavigate: (OpenSourcePage, String?) -> Unit,
) {
  val uriHandler = LocalUriHandler.current
  val notifier = koinInject<ToastGlobalNotifier>()
  Scaffold(topBar = {
    CenteredTopAppBar(title = stringResource(Res.string.about_open_source_notices), navigate = onBack)
  }) { padding ->
    NoticesWebView(
      page = page,
      anchor = anchor,
      appInfo = appInfo,
      onNavigate = onNavigate,
      onExternalLink = { url ->
        try {
          uriHandler.openUri(url)
        } catch (_: IllegalArgumentException) {
          notifier.showToast(Res.string.about_notices_open_failed)
        }
      },
      onError = { notifier.showToast(Res.string.about_notices_open_failed) },
      modifier = Modifier.fillMaxSize().padding(padding),
    )
  }
}

@Composable
internal expect fun NoticesWebView(
  page: OpenSourcePage,
  anchor: String?,
  appInfo: AboutAppInfo,
  onNavigate: (OpenSourcePage, String?) -> Unit,
  onExternalLink: (String) -> Unit,
  onError: () -> Unit,
  modifier: Modifier = Modifier,
)
