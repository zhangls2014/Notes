package me.zhangls.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.material3.Icon
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import me.zhangls.main.api.FavoritesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.SettingsDestination
import me.zhangls.main.api.TabDestination
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.icon.Favorite
import me.zhangls.theme.icon.Home
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Settings
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
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
 * 外壳消费导航套件占用的系统内边距，并将页面绘制裁剪在内容区内
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
    // 页面滑动允许超出自身布局边界，但不能越过内容区覆盖导航侧栏。
    Box(
      Modifier
        .consumeWindowInsets(layoutType.navigationSuiteInsets())
        .clipToBounds(),
    ) {
      content()
    }
  }
}

/**
 * Navigation is a single-pane surface: only window size controls its placement.
 * Passing the changing tabletop posture to the library would switch rail/bar at
 * identical window geometry, moving every content pane when a fold opens.
 * Physical hinge avoidance remains the responsibility of pane layouts.
 */
fun WindowAdaptiveInfo.navigationSuiteType(): NavigationSuiteType {
  // 矮而宽的窗口优先保留内容高度；即使达到 ExtraLarge 也不展开侧栏。
  if (
    windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) &&
    !windowSizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
  ) {
    return NavigationSuiteType.WideNavigationRailCollapsed
  }
  val sizeOnlyInfo = WindowAdaptiveInfo(windowSizeClass, Posture())
  val recommended = NavigationSuiteScaffoldDefaults.navigationSuiteType(sizeOnlyInfo)
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

/**
 * 多形态预览：同一份 `AppShell` 在四档宽度下的形态。
 *
 * 用 [ProvideWindowAdaptiveInfo] 直接注入窗口形态，而不是靠预览面板的尺寸 ——
 * 形态由尺寸类决定，预览面板只负责让画布够大。这也是 `@Preview` 与真机
 * `wm size` / iPad 分屏之外最省事的一条验证路径。
 */
@Preview(name = "compact 360dp", widthDp = 400, heightDp = 800)
@Composable
private fun AppShellCompactPreview() = AppShellPreview(widthDp = 360, heightDp = 800)

@Preview(name = "landscape 800x400dp", widthDp = 800, heightDp = 400)
@Composable
private fun AppShellLandscapePreview() = AppShellPreview(widthDp = 800, heightDp = 400)

@Preview(name = "medium 700dp", widthDp = 700, heightDp = 900)
@Composable
private fun AppShellMediumPreview() = AppShellPreview(widthDp = 700, heightDp = 1000)

@Preview(name = "expanded 1000dp", widthDp = 1000, heightDp = 800)
@Composable
private fun AppShellExpandedPreview() = AppShellPreview(widthDp = 1000, heightDp = 800)

@Preview(name = "extraLarge 1600dp", widthDp = 1200, heightDp = 800)
@Composable
private fun AppShellExtraLargePreview() = AppShellPreview(widthDp = 1600, heightDp = 900)

@Composable
private fun AppShellPreview(widthDp: Int, heightDp: Int) {
  ProvideWindowAdaptiveInfo(
    // 与库同一套量化方式：从实际 dp 推出档位，而不是手写档位下限 ——
    // 手写下限会把"档位是怎么算出来的"这个被测对象本身写进测试前提里
    windowSizeClass = WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(widthDp, heightDp),
  ) {
    ComposeAppTheme {
      AppShell(selected = HomeDestination, onSelectTab = {}) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(text = "content")
        }
      }
    }
  }
}
