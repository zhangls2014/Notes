package me.zhangls.about

import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.zhangls.about.api.AboutDestination
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.OpenSourceNoticesDestination
import me.zhangls.about.api.OpenSourcePage
import me.zhangls.framework.nav.NavEffect
import me.zhangls.framework.toast.ToastGlobalNotifier
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_notices_open_failed
import org.koin.compose.koinInject

fun EntryProviderScope<NavKey>.aboutNavEntry(
  paneMetadata: (OpenSourceNoticesDestination) -> Map<String, Any>,
  embedProject: () -> Boolean,
  effect: (NavEffect) -> Unit,
) {
  entry<AboutDestination> {
    koinInject<AboutEntry>().Screen(
      onBack = { effect(NavEffect.Popup) },
      onOpenSource = { effect(NavEffect.Navigate(OpenSourceNoticesDestination())) },
    )
  }
  entry<OpenSourceNoticesDestination>(metadata = paneMetadata) { destination ->
    val uriHandler = LocalUriHandler.current
    val notifier = koinInject<ToastGlobalNotifier>()
    val projectUrl = destination.projectUrl
    if (projectUrl != null) {
      koinInject<AboutEntry>().ProjectScreen(projectUrl, onBack = { effect(NavEffect.Popup) })
      return@entry
    }
    koinInject<AboutEntry>().NoticesScreen(
      page = destination.page,
      anchor = destination.anchor,
      onBack = { effect(NavEffect.Popup) },
      onExternalLink = { url ->
        if (embedProject()) {
          effect(NavEffect.Navigate(OpenSourceNoticesDestination(projectUrl = url)))
        } else {
          try {
            uriHandler.openUri(url)
          } catch (_: IllegalArgumentException) {
            notifier.showToast(Res.string.about_notices_open_failed)
          }
        }
      },
      onNavigate = { page, anchor ->
        if (page == OpenSourcePage.LIBRARIES && destination.page == OpenSourcePage.LICENSES) {
          effect(NavEffect.Popup)
        } else {
          effect(NavEffect.Navigate(OpenSourceNoticesDestination(page, anchor)))
        }
      },
    )
  }
}
