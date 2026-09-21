package me.zhangls.main.api

import kotlinx.serialization.modules.SerializersModule
import me.zhangls.framework.deeplink.DeepLinkMatcher
import me.zhangls.framework.nav.NavigationContribution
import me.zhangls.framework.nav.registerDestination

private const val PATH_HOME = "/home"

/**
 * 主界面 feature 对宿主导航的贡献：三个 Tab 的 key 登记 + `https://<host>/home`。
 */
object MainNavigation : NavigationContribution {
  override val navModule: SerializersModule = SerializersModule {
    registerDestination(HomeDestination::class, HomeDestination.serializer())
    registerDestination(FavoritesDestination::class, FavoritesDestination.serializer())
    registerDestination(SettingsDestination::class, SettingsDestination.serializer())
  }

  override val deepLinkMatchers: List<DeepLinkMatcher> = listOf(
    DeepLinkMatcher { request -> if (request.path == PATH_HOME) HomeDestination else null }
  )
}
