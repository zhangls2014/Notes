package me.zhangls.email.api

import kotlinx.serialization.modules.SerializersModule
import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.deeplink.DeepLinkMatcher
import me.zhangls.framework.deeplink.DeepLinkRequest
import me.zhangls.framework.nav.NavigationContribution
import me.zhangls.framework.nav.registerDestination

private const val PATH_EMAIL = "/email"
private const val QUERY_ID = "id"

/**
 * 邮件 feature 对宿主导航的贡献。
 *
 * 路径表与目的地定义放在同一处：改 key、改路径时两边一起改，不会出现"宿主里的路径字符串还在
 * 指向旧的 key"这种编译期无提示的漂移。
 */
object EmailNavigation : NavigationContribution {
  override val navModule: SerializersModule = SerializersModule {
    registerDestination(EmailDetailDestination::class, EmailDetailDestination.serializer())
  }

  /** `https://<host>/email?id=<邮件 id>`；`id` 缺失或非法时视为不匹配。 */
  override val deepLinkMatchers: List<DeepLinkMatcher> = listOf(
    DeepLinkMatcher { request -> request.toEmailDetail() }
  )

  private fun DeepLinkRequest.toEmailDetail(): DeepLinkDestination? {
    if (path != PATH_EMAIL) return null
    return queries[QUERY_ID]?.toLongOrNull()?.let { EmailDetailDestination(emailId = it) }
  }
}
