package me.zhangls.framework.nav

import kotlinx.serialization.modules.SerializersModule
import me.zhangls.framework.deeplink.DeepLinkMatcher

/**
 * 一个 feature 对宿主导航的序列化登记与 DeepLink 匹配贡献。
 *
 * 契约模块（`-api`）只负责声明，实现在各自的 Koin 模块里以**本接口类型**登记
 * （`@Single @Named("<feature>NavigationContribution")`），组合根用 `getAll`
 * 一次收集、统一装配。
 *
 * 各 feature 的 qualifier 必须唯一：Koin 按类型、qualifier 与作用域索引定义，
 * 仅使用不同的 provider 函数名不能区分同接口绑定。
 *
 * 为什么要收成一个接口而不是让组合根分别收集两个列表：导航 key 的序列化登记与它的 DeepLink
 * 匹配是**同一件事的两面**（都是"这个 feature 有哪些导航目的地"）。分成两处收集意味着新增一个
 * 目的地要在两份清单里各加一次，而漏掉任何一处的后果都只在运行期暴露：
 * 漏登记 key → 重建时 `SerializationException`；漏匹配器 → 链接静默打不开。
 *
 * 新增贡献无需修改组合根的收集逻辑；Feature 实现、Nav Entry 与必要的根导航策略
 * 仍由 composeApp 显式装配。
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
