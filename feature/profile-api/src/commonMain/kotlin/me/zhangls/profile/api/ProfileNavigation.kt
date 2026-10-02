package me.zhangls.profile.api

import kotlinx.serialization.modules.SerializersModule
import me.zhangls.framework.deeplink.DeepLinkMatcher
import me.zhangls.framework.nav.NavigationContribution
import me.zhangls.framework.nav.registerDestination

object ProfileNavigation : NavigationContribution {
  override val navModule = SerializersModule {
    registerDestination(ProfileDestination::class, ProfileDestination.serializer())
  }
  override val deepLinkMatchers: List<DeepLinkMatcher> = emptyList()
}
