package me.zhangls.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.ktor.http.URLBuilder
import me.zhangls.about.api.NotesWebsite
import me.zhangls.about.api.OpenSourcePage
import me.zhangls.framework.AppInfo
import me.zhangls.framework.toast.ToastGlobalNotifier
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_copyright_notices
import notes.feature.about.generated.resources.about_license
import notes.feature.about.generated.resources.about_notices_open_failed
import notes.feature.about.generated.resources.about_open_source_notices
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
internal fun OpenSourceNoticesScreen(
  page: OpenSourcePage,
  anchor: String?,
  onBack: () -> Unit,
  onNavigate: (OpenSourcePage, String?) -> Unit,
  onExternalLink: (String) -> Unit,
  site: NotesWebsite,
) {
  val notifier = koinInject<ToastGlobalNotifier>()
  val appearance = currentNoticesAppearance()
  val appInfo = koinInject<AppInfo>()
  val onlineUrl = appInfo.onlineNoticesPath()?.takeIf { page == OpenSourcePage.LIBRARIES }?.let { path ->
    URLBuilder("${site.origin}$path").apply {
      appearance.noticesAppearanceParameters().forEach { (key, value) -> parameters.append(key, value) }
    }.buildString()
  }
  Scaffold(topBar = {
    val title = when {
      page == OpenSourcePage.LIBRARIES -> stringResource(Res.string.about_open_source_notices)
      anchor?.startsWith("license-") == true -> stringResource(Res.string.about_license)
      else -> stringResource(Res.string.about_copyright_notices)
    }
    NoticesTopAppBar(title = title, onBack = onBack, isDetail = page != OpenSourcePage.LIBRARIES, onlineUrl = onlineUrl)
  }) { padding ->
    NoticesWebView(
      page = page,
      anchor = anchor,
      appearance = appearance,
      onNavigate = onNavigate,
      onExternalLink = onExternalLink,
      onError = { notifier.showToast(Res.string.about_notices_open_failed) },
      bottomInset = padding.calculateBottomPadding(),
      modifier = Modifier.fillMaxSize().padding(padding.webViewContentPadding(LocalLayoutDirection.current))
        .testTag("notices-webview"),
    )
  }
}

@Composable
internal expect fun NoticesWebView(
  page: OpenSourcePage,
  anchor: String?,
  appearance: NoticesAppearance,
  onNavigate: (OpenSourcePage, String?) -> Unit,
  onExternalLink: (String) -> Unit,
  onError: () -> Unit,
  modifier: Modifier = Modifier,
  bottomInset: Dp = 0.dp,
)
