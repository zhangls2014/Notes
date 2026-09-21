package me.zhangls.main

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.zhangls.email.api.EmailEntry
import me.zhangls.main.api.FavoritesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.SettingsDestination
import me.zhangls.settings.api.SettingsEntry
import me.zhangls.settings.api.SettingsResult
import org.koin.compose.koinInject

/**
 * 主界面三个 Tab 的导航条目。
 *
 * "哪个 Tab 放哪个 feature 的内容"是主界面的知识，集中在这里；email 与 settings 只提供内容，
 * 不关心自己被放在哪一个 Tab 里。
 *
 * 跨 feature 的跳转直接表达为回调参数（如 [navigateToEmailDetail] 携带邮件 id），
 * 而不是把路由降级成宿主可识别的"结果"再翻译回 key —— 后者要求每加一条路由就改三处。
 *
 * key 与它们的序列化登记都在契约模块（[me.zhangls.main.api.MainNavigation]）。
 *
 * @param navigateToEmailDetail 收藏列表点击邮件
 * @param onLogout 设置页登出
 */
fun EntryProviderScope<NavKey>.mainNavEntries(
  navigateToEmailDetail: (Long) -> Unit,
  onLogout: () -> Unit,
) {
  entry<HomeDestination> {
    koinInject<EmailEntry>().HomeScreen()
  }

  entry<FavoritesDestination> {
    koinInject<EmailEntry>().FavoritesScreen(navigateToDetail = navigateToEmailDetail)
  }

  entry<SettingsDestination> {
    koinInject<SettingsEntry>().Screen { result ->
      if (result == SettingsResult.Logout) onLogout()
    }
  }
}
