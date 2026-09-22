package me.zhangls.email.api

import kotlinx.serialization.Serializable
import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.nav.RequireLogin

/**
 * 邮件详情页的导航 key。
 *
 * 定义在契约模块而不是实现模块：Nav3 的模型是"key 即路由"，key 是纯 `@Serializable` 数据、
 * 零 Compose 依赖，本可以放在契约层；而它正是兄弟 feature 需要的那个东西（跨 feature 导航 =
 * 构造对方的 key）。留在实现模块里，兄弟 feature 就只能拿到实现模块，或者退化成传裸参数
 * （如 `Long`）再由宿主翻译回 key —— 后者每加一条路由要改三处。
 *
 * @property emailId 邮件 id
 * @property scene 从哪个列表打开的（也就是它属于哪一套列表-详情分栏）。
 *   见 [EmailListScene]：它决定分栏归属，也决定返回时回到哪个列表。
 *   DeepLink 直达时用默认值 —— 外部链接没有"来自哪个列表"的概念。
 */
@Serializable
data class EmailDetailDestination(
  val emailId: Long,
  val scene: EmailListScene = EmailListScene.Home,
) : DeepLinkDestination, RequireLogin
