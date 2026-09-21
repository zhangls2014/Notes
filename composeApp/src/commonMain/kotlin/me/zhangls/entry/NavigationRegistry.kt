package me.zhangls.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import me.zhangls.framework.deeplink.DeepLinkMatcher
import me.zhangls.framework.nav.NavigationContribution
import org.koin.compose.currentKoinScope

/**
 * 本应用收集到的全部导航贡献。
 *
 * 各 feature 在自己的 Koin 模块里以契约接口类型登记（`@Single fun …(): NavigationContribution`），
 * 这里一处收集。组合根因此不持有任何 feature 的路由知识 —— 新增 feature 时它零改动。
 *
 * 早先这两样东西由组合根手工拼接：`mainNavModule + loginNavModule + emailNavModule`，以及文件内
 * 私有的 `when (request.path)` 路径表。漏掉任何一项都只在运行期暴露（重建时
 * `SerializationException`，或链接静默打不开），编译期毫无提示。
 *
 * 注意 `getAll` 而不是注入 `List<NavigationContribution>`：Koin 的集合解析并不把同类型的多个
 * 定义汇总成一个 `List` 定义，注入 `List` 会抛 `NoDefinitionFoundException`。
 */
class NavigationRegistry(contributions: List<NavigationContribution>) {
  init {
    // 收集为空几乎总是接线问题（各 feature 忘记登记），而它的症状是 DeepLink 静默失效 ——
    // 没有任何报错。这里让它立刻失败，而不是等使用者发现链接打不开。
    require(contributions.isNotEmpty()) {
      "没有收集到任何 NavigationContribution：各 feature 是否忘记在 Koin 模块里登记？"
    }
  }

  /** 折叠后的返回栈序列化注册表。 */
  val navModule: SerializersModule =
    contributions.fold(SerializersModule { }) { acc, contribution -> acc + contribution.navModule }

  /** 全部 DeepLink 匹配器；按 feature 的登记顺序依次尝试。 */
  val deepLinkMatchers: List<DeepLinkMatcher> =
    contributions.flatMap { it.deepLinkMatchers }
}

@Composable
fun rememberNavigationRegistry(): NavigationRegistry {
  val scope = currentKoinScope()
  return remember(scope) { NavigationRegistry(scope.getAll()) }
}
