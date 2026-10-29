package me.zhangls.about.api

import kotlinx.serialization.modules.SerializersModule
import me.zhangls.framework.deeplink.DeepLinkMatcher
import me.zhangls.framework.nav.NavigationContribution
import me.zhangls.framework.nav.registerDestination

object AboutNavigation : NavigationContribution {
  override val navModule = SerializersModule {
    registerDestination(AboutDestination::class, AboutDestination.serializer())
    registerDestination(ReleaseHistoryDestination::class, ReleaseHistoryDestination.serializer())
    registerDestination(OpenSourceNoticesDestination::class, OpenSourceNoticesDestination.serializer())
  }
  override val deepLinkMatchers: List<DeepLinkMatcher> = emptyList()
}
