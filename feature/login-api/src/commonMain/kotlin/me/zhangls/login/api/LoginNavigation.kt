package me.zhangls.login.api

import kotlinx.serialization.modules.SerializersModule
import me.zhangls.framework.deeplink.DeepLinkMatcher
import me.zhangls.framework.nav.NavigationContribution
import me.zhangls.framework.nav.registerDestination

/**
 * 登录 feature 对宿主导航的贡献。
 *
 * 没有 DeepLink 入口：登录页只能由登录守卫压入，不接受外部链接直达。
 */
object LoginNavigation : NavigationContribution {
  override val navModule: SerializersModule = SerializersModule {
    registerDestination(LoginDestination::class, LoginDestination.serializer())
  }

  override val deepLinkMatchers: List<DeepLinkMatcher> = emptyList()
}
