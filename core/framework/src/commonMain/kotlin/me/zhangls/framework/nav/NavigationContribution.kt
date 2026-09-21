package me.zhangls.framework.nav

import kotlinx.serialization.modules.SerializersModule
import me.zhangls.framework.deeplink.DeepLinkMatcher

/**
 * 一个 feature 对**宿主导航**的全部贡献。
 *
 * 契约模块（`-api`）只负责声明，实现在各自的 Koin 模块里以**本接口类型**登记
 * （`@Single fun xxxNavigationContribution(): NavigationContribution`），组合根用 `getAll`
 * 一次收集、统一装配。
 *
 * 登记函数名在各 feature 之间必须唯一：同名同返回类型会让 Koin 里出现多个同名同类型的定义，
 * 收集结果不可靠。
 *
 * 为什么要收成一个接口而不是让组合根分别收集两个列表：导航 key 的序列化登记与它的 DeepLink
 * 匹配是**同一件事的两面**（都是"这个 feature 有哪些导航目的地"）。分成两处收集意味着新增一个
 * 目的地要在两份清单里各加一次，而漏掉任何一处的后果都只在运行期暴露：
 * 漏登记 key → 重建时 `SerializationException`；漏匹配器 → 链接静默打不开。
 *
 * 收成一个接口后，"新增一个 feature 的导航"在组合根侧是零改动 —— 它只认这个接口。
 */
interface NavigationContribution {
  /**
   * 本 feature 全部导航 key 的序列化登记，由组合根折叠进 `SavedStateConfiguration`。
   *
   * 用 [registerDestination] 登记，它同时覆盖 `NavKey` 与 `Destination` 两个多态基类。
   */
  val navModule: SerializersModule

  /** 本 feature 负责的 DeepLink 路径匹配器；没有 DeepLink 支持时为空。 */
  val deepLinkMatchers: List<DeepLinkMatcher>
}
