package me.zhangls.main

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import me.zhangls.main.api.FavoritesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.SettingsDestination
import me.zhangls.main.api.TabDestination
import me.zhangls.theme.icon.Favorite
import me.zhangls.theme.icon.Home
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Settings
import me.zhangls.theme.layout.LocalNavigationPlacement
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo
import me.zhangls.theme.layout.NavigationPlacement
import me.zhangls.theme.layout.isExtraLargeWidthOrWider
import notes.feature.main.generated.resources.Res
import notes.feature.main.generated.resources.main_label_favorites
import notes.feature.main.generated.resources.main_label_home
import notes.feature.main.generated.resources.main_label_settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 一个 Tab 的展示信息。顺序即导航套件里的顺序。
 */
private enum class MainTab(
  val destination: TabDestination,
  val label: StringResource,
  val icon: ImageVector,
) {
  HOME(HomeDestination, Res.string.main_label_home, Icons.Rounded.Home),
  FAVORITES(FavoritesDestination, Res.string.main_label_favorites, Icons.Rounded.Favorite),
  SETTINGS(SettingsDestination, Res.string.main_label_settings, Icons.Rounded.Settings),
}

/**
 * 应用外壳：导航套件（Rail / 底部导航栏）以及它对内容区的承诺。
 *
 * 外壳属于**应用**而不是某个目的地 —— 它是 `NavDisplay` 的容器，不是返回栈里的一个条目。
 * 早先它是主界面目的地内部的一部分，于是推入详情页时 `SinglePaneSceneStrategy` 只渲染栈顶，
 * Rail / 底部栏随主界面一起消失：宽屏上"列表 + 详情"会突然变成全屏页，导航套件不再可达；
 * 同一个用户动作（点邮件）在首页与收藏页的表现还不一致（前者走分栏、后者走目的地）。
 *
 * 提到 `NavDisplay` 之外后，`NavigationPlacement` 也就不再需要"脱离外壳单独渲染"这个兜底场景：
 * 除登录页外，所有内容都在外壳内。
 *
 * @param selected 当前选中的 Tab，由返回栈决定；为 `null` 表示当前不在任何 Tab 内
 *   （如登录页），此时不显示导航套件
 * @param onSelectTab 点击某个 Tab
 * @param content 外壳内容，即 `NavDisplay`
 */
@Composable
fun AppShell(
  selected: TabDestination?,
  onSelectTab: (TabDestination) -> Unit,
  content: @Composable () -> Unit,
) {
  if (selected == null) {
    content()
    return
  }

  // 窗口形态由组合根算一次后下发（`App` → `LocalWindowAdaptiveInfo`）。
  // 外壳不自己算：一个窗口只有一种形态，两处各算一次会在分屏拖拽 / 折叠时得到互相矛盾的答案。
  val adaptiveInfo = LocalWindowAdaptiveInfo.current
  val layoutType = remember(adaptiveInfo) { adaptiveInfo.navigationSuiteType() }
  val navigationPlacement = remember(layoutType) { layoutType.toNavigationPlacement() }

  NavigationSuiteScaffold(
    navigationSuiteItems = {
      MainTab.entries.forEach { tab ->
        item(
          icon = { Icon(imageVector = tab.icon, contentDescription = stringResource(tab.label)) },
          label = { Text(stringResource(tab.label)) },
          selected = tab.destination == selected,
          onClick = { onSelectTab(tab.destination) },
        )
      }
    },
    layoutType = layoutType,
  ) {
    // 导航方位在此下发：各 feature 的内容区据此换算内边距，契约里因此不需要任何布局参数
    CompositionLocalProvider(LocalNavigationPlacement provides navigationPlacement) {
      content()
    }
  }
}

/**
 * 导航套件该用哪种形态。
 *
 * 交给库的推荐策略 [NavigationSuiteScaffoldDefaults.navigationSuiteType]，本应用只对
 * **与库默认不同**的那一档做显式覆盖。原先这里是一条手写分档链，三个问题：
 *
 * 1. **口径错**：`WindowSizeClass.minWidthDp` / `minHeightDp` 是**分档下限**，不是窗口宽高 ——
 *    档位是按"不超过实际宽度的最大档位下限"量化出来的，于是拿这两个值互相比较来推断
 *    "是不是横屏"，比较的其实是 600 与 480/900 两组被量化过的数。库的策略用
 *    `minWidth` / `minHeight` 与 `windowPosture` 表达，语义是对的。
 * 2. **漏姿态**：手写链不看 `windowPosture.isTabletop`，折叠设备半开时会给出 Rail，
 *    而这是最不该给 Rail 的场景（竖向空间被铰链切断）。
 * 3. **有死分支**：原先第一档判断 `isWidthAtLeastBreakpoint(1200)`，而 V1 档位集下
 *    `minWidthDp` 最大只有 840，`840 >= 1200` 恒假 —— `WideNavigationRailExpanded` 写了却
 *    永远不可达。改用 V2 后该分支才第一次真正可命中，因此本函数把它显式表达出来。
 */
private fun WindowAdaptiveInfo.navigationSuiteType(): NavigationSuiteType {
  val recommended = NavigationSuiteScaffoldDefaults.navigationSuiteType(this)
  return if (recommended == NavigationSuiteType.WideNavigationRailCollapsed && isExtraLargeWidthOrWider) {
    NavigationSuiteType.WideNavigationRailExpanded
  } else {
    recommended
  }
}

/**
 * 把 [NavigationSuiteScaffold] 的布局类型归类为"导航套件占哪一侧"。
 *
 * 归类结果是宿主布局对内容区唯一的承诺，经 `LocalNavigationPlacement` 下发。
 * 原先这里是一个 `isBottomNavigationBar: Boolean`，穿透了 3 个 feature 的公开契约、
 * 在 15 个文件间手工传递 47 次，且被三处当成了三种不同含义（内边距 / 搜索栏形态 / 屏幕尺寸）。
 *
 * 这里**正面列出"占侧边"的形态**，而不是反向列出"底部那三个"。原写法把
 * [NavigationSuiteType.NavigationDrawer]（模态抽屉，覆盖在内容之上、并不占位）与
 * [NavigationSuiteType.None]（不渲染导航套件）都扫进了 `else -> Side`，
 * 内容会因此白白让出一侧内边距。
 *
 * `else` 无法避免：[NavigationSuiteType] 是 `@JvmInline value class`（内部包一个 String），
 * 不是 enum，`when` 不具备穷尽性检查。
 */
private fun NavigationSuiteType.toNavigationPlacement(): NavigationPlacement = when (this) {
  NavigationSuiteType.NavigationRail,
  NavigationSuiteType.WideNavigationRailCollapsed,
  NavigationSuiteType.WideNavigationRailExpanded,
  -> NavigationPlacement.Side

  else -> NavigationPlacement.Bottom
}
