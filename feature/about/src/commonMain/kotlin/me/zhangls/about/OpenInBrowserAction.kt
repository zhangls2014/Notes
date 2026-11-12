package me.zhangls.about

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import me.zhangls.framework.toast.ToastGlobalNotifier
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Language
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_browser_failed
import notes.feature.about.generated.resources.about_open_in_browser
import org.jetbrains.compose.resources.stringResource
import org.koin.mp.KoinPlatform

@Composable
internal fun OpenInBrowserAction(url: String) {
  val openInBrowser = rememberOpenInBrowser()
  IconButton(onClick = { openInBrowser(url) }) {
    Icon(Icons.Rounded.Language, contentDescription = stringResource(Res.string.about_open_in_browser))
  }
}

@Composable
internal fun rememberOpenInBrowser(): (String) -> Unit {
  val handler = LocalUriHandler.current
  return { url ->
    try {
      handler.openUri(url)
    } catch (_: IllegalArgumentException) {
      KoinPlatform.getKoinOrNull()?.getOrNull<ToastGlobalNotifier>()?.showToast(Res.string.about_browser_failed)
    }
  }
}
