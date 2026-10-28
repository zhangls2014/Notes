package me.zhangls.about

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.about.api.AboutDestination
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.OpenSourceNoticesDestination
import me.zhangls.about.api.OpenSourcePage
import me.zhangls.framework.nav.NavEffect
import org.koin.compose.koinInject

fun EntryProviderScope<NavKey>.aboutNavEntry(appInfo: () -> AboutAppInfo, effect: (NavEffect) -> Unit) {
  entry<AboutDestination> {
    koinInject<AboutEntry>().Screen(
      appInfo(),
      onBack = { effect(NavEffect.Popup) },
      onOpenSource = { effect(NavEffect.Navigate(OpenSourceNoticesDestination())) },
    )
  }
  entry<OpenSourceNoticesDestination> { destination ->
    koinInject<AboutEntry>().NoticesScreen(
      page = destination.page,
      anchor = destination.anchor,
      appInfo = appInfo(),
      onBack = { effect(NavEffect.Popup) },
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
