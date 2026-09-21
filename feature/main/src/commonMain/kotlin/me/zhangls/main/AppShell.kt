package me.zhangls.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.material3.Icon
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import me.zhangls.main.api.FavoritesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.SettingsDestination
import me.zhangls.main.api.TabDestination
import me.zhangls.theme.icon.Favorite
import me.zhangls.theme.icon.Home
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Settings
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo
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
 * **外壳对内容区唯一的承诺，是把导航套件占用的那部分系统内边距消费掉**
 * （见 [navigationSuiteInsets]）。内容区因此不需要知道"导航在哪一侧"、
 * 更不需要自己换算"哪几边该少留白" —— 各页面直接使用 `Scaffold` 给出的 padding 即可。
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
    Box(Modifier.consumeWindowInsets(layoutType.navigationSuiteInsets())) {
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
  return if (
    recommended == NavigationSuiteType.WideNavigationRailCollapsed && isExtraLargeWidthOrWider
  ) {
    NavigationSuiteType.WideNavigationRailExpanded
  } else {
    recommended
  }
}

/**
 * 导航套件自己占用的那部分系统内边距。外壳必须把它**消费**掉，而不是像原先那样
 * "约定内容区少留白某一侧"（`NavigationPlacement` + `toContentPadding`）。
 *
 * 两个理由：
 *
 * 1. **库没消费**。`NavigationSuiteScaffold` 只对 `NavigationBar` / `NavigationRail` /
 *    `NavigationDrawer` 三种旧形态调用了 `consumeWindowInsets`；本应用实际使用的
 *    `ShortNavigationBar*` 与 `WideNavigationRail*` 全部落在它的 `else -> NoWindowInsets`
 *    分支上 —— 什么都不消费。若外壳不补上，内容区的 `Scaffold` 会把底部（或左侧）系统栏
 *    内边距再算一遍，与导航套件重复留白。
 * 2. **归零一侧是第二套真相**。原先"哪几边保留"由消费方各自换算，注释里也承认这份逻辑
 *    "原先在三个消费方各写了一遍并已漂移"；它当下没出问题，只是因为"库不消费"与
 *    "无条件归零那一侧"两件互相不知道的事凑巧互补。而且它把 `LayoutDirection.Ltr` 写死，
 *    RTL 下 Rail 落在右侧时内容会被压住。
 *
 * 消费之后内容区读到 0，各页面直接用 `Scaffold` 给的 padding。方向语义由
 * `WindowInsetsSides` 表达，RTL 问题不再存在。
 */
@Composable
private fun NavigationSuiteType.navigationSuiteInsets(): WindowInsets = when (this) {
  NavigationSuiteType.ShortNavigationBarCompact,
  NavigationSuiteType.ShortNavigationBarMedium,
  -> ShortNavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)

  NavigationSuiteType.WideNavigationRailCollapsed,
  NavigationSuiteType.WideNavigationRailExpanded,
  -> WideNavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)

  // 旧三形态由 NavigationSuiteScaffold 自己消费；None 表示不渲染导航套件。
  // 这里再消费一次不会出错（consumeWindowInsets 会与已有值相减并夹到 0），但没必要。
  else -> WindowInsets(0, 0, 0, 0)
}
