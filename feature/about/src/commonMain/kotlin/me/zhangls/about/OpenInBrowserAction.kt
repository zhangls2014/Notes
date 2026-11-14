package me.zhangls.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import me.zhangls.framework.toast.ToastGlobalNotifier
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_browser_failed
import org.koin.mp.KoinPlatform

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
