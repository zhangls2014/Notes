package me.zhangls.about

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.zhangls.about.api.AboutDestination
import me.zhangls.about.api.AboutEntry
import me.zhangls.about.api.CacheManagementDestination
import me.zhangls.about.api.OfficialWebsiteDestination
import me.zhangls.about.api.OnlineNoticeDetailDestination
import me.zhangls.about.api.OnlineNoticesDestination
import me.zhangls.about.api.ReleaseHistoryDestination
import me.zhangls.framework.nav.NavEffect
import org.koin.compose.koinInject

fun EntryProviderScope<NavKey>.aboutNavEntry(
  listMetadata: Map<String, Any>,
  detailMetadata: Map<String, Any>,
  effect: (NavEffect) -> Unit,
) {
  val openDetail: (OnlineNoticeDetailDestination) -> Unit = { effect(NavEffect.Navigate(it)) }
  entry<AboutDestination> {
    koinInject<AboutEntry>().Screen(
      onBack = { effect(NavEffect.Popup) },
      onOpenReleaseHistory = { effect(NavEffect.Navigate(ReleaseHistoryDestination)) },
      onOpenNotices = { effect(NavEffect.Navigate(OnlineNoticesDestination)) },
      onOpenWebsite = { effect(NavEffect.Navigate(OfficialWebsiteDestination)) },
      onOpenCacheManagement = { effect(NavEffect.Navigate(CacheManagementDestination)) },
    )
  }
  entry<OnlineNoticesDestination>(metadata = listMetadata) {
    koinInject<AboutEntry>().OnlineNoticesScreen(
      detail = null,
      onBack = { effect(NavEffect.Popup) },
      onOpenDetail = openDetail,
    )
  }
  entry<OnlineNoticeDetailDestination>(metadata = detailMetadata) { destination ->
    koinInject<AboutEntry>().OnlineNoticesScreen(
      detail = destination,
      onBack = { effect(NavEffect.Popup) },
      onOpenDetail = openDetail,
    )
  }
  entry<OfficialWebsiteDestination> {
    koinInject<AboutEntry>().OfficialWebsiteScreen(onBack = { effect(NavEffect.Popup) })
  }
  entry<CacheManagementDestination> {
    koinInject<AboutEntry>().CacheManagementScreen(onBack = { effect(NavEffect.Popup) })
  }
  entry<ReleaseHistoryDestination> {
    koinInject<AboutEntry>().ReleaseHistoryScreen(onBack = { effect(NavEffect.Popup) })
  }
}
