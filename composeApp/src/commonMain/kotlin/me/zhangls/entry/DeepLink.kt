package me.zhangls.entry

import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.deeplink.DeepLinkMatcher
import me.zhangls.framework.deeplink.parseDeepLinkUrl

private const val HTTPS = "https"
private const val NOTES_HOST = "notes.zhangls.me"

/**
 * 把 DeepLink URL 解析成本应用的目的地。
 *
 * 本函数只判断"这是不是本应用的链接"（scheme + host）。**路径表不在这里** —— 每条路径与它对应的
 * key 一起定义在各 feature 的 `-api`（`DeepLinkMatcher`），由组合根收集后传入。
 *
 * 早先路径表硬编码在这里的 `when (request.path)` 里：key 改名不会有编译错误，只会在运行期静默
 * 失配（链接打不开，无任何提示）。路径与 key 同处之后，这类漂移不复存在。
 *
 * @param matchers 各 feature 的匹配器，按序尝试，第一个匹配成功者胜出
 */
internal fun parseDeepLink(
  url: String?,
  matchers: List<DeepLinkMatcher>,
): DeepLinkDestination? {
  val request = parseDeepLinkUrl(url) ?: return null
  if (request.scheme != HTTPS) return null
  if (request.host != NOTES_HOST) return null

  return matchers.firstNotNullOfOrNull { it.match(request) }
}
