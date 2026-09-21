package me.zhangls.framework.deeplink

/**
 * 把一个已解析的 DeepLink 请求匹配成本模块的导航 key。
 *
 * 每个 feature 在各自的 `-api` 里提供一份，**路径表因此与目的地定义同处**：改路径或改 key 时
 * 两边在同一处修改，不会漏。组合根只按序尝试，不再持有任何具体路径字符串 —— 早先路径表硬编码
 * 在组合根的 `when (request.path)` 里，key 改名不会有编译错误，只会在运行时静默失配。
 *
 * 返回 `null` 表示"这条路径不归我管"，由下一个匹配器继续尝试。
 */
fun interface DeepLinkMatcher {
  fun match(request: DeepLinkRequest): DeepLinkDestination?
}
