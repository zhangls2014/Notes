package me.zhangls.main

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.window.core.layout.WindowSizeClass
import me.zhangls.main.api.FavoritesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.SettingsDestination
import me.zhangls.main.api.TabDestination
import me.zhangls.theme.icon.Favorite
import me.zhangls.theme.icon.Home
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Settings
import me.zhangls.theme.layout.LocalNavigationPlacement
import me.zhangls.theme.layout.NavigationPlacement
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

  val adaptiveInfo = currentWindowAdaptiveInfo()
  val windowSizeClass = adaptiveInfo.windowSizeClass
  // 布局类型仅由窗口尺寸类决定，用 remember 避免每次重组重复计算
  val customLayoutType = remember(windowSizeClass) {
    with(windowSizeClass) {
      if (isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND)) {
        NavigationSuiteType.WideNavigationRailExpanded
      } else if (isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)) {
        NavigationSuiteType.WideNavigationRailCollapsed
      } else if (isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)) {
        if (minWidthDp > minHeightDp) {
          NavigationSuiteType.WideNavigationRailCollapsed
        } else {
          NavigationSuiteType.ShortNavigationBarMedium
        }
      } else {
        NavigationSuiteType.ShortNavigationBarCompact
      }
    }
  }
  val navigationPlacement = remember(customLayoutType) { customLayoutType.toNavigationPlacement() }

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
    layoutType = customLayoutType,
  ) {
    // 导航方位在此下发：各 feature 的内容区据此换算内边距，契约里因此不需要任何布局参数
    CompositionLocalProvider(LocalNavigationPlacement provides navigationPlacement) {
      content()
    }
  }
}

/**
 * 把 [NavigationSuiteScaffold] 的布局类型归类为"导航套件占哪一侧"。
 *
 * 归类结果是宿主布局对内容区唯一的承诺，经 `LocalNavigationPlacement` 下发。
 * 原先这里是一个 `isBottomNavigationBar: Boolean`，穿透了 3 个 feature 的公开契约、
 * 在 15 个文件间手工传递 47 次，且被三处当成了三种不同含义（内边距 / 搜索栏形态 / 屏幕尺寸）。
 */
private fun NavigationSuiteType.toNavigationPlacement(): NavigationPlacement {
  val bottomNavigationTypes = arrayOf(
    NavigationSuiteType.ShortNavigationBarCompact,
    NavigationSuiteType.ShortNavigationBarMedium,
    NavigationSuiteType.NavigationBar
  )
  return if (bottomNavigationTypes.contains(this)) {
    NavigationPlacement.Bottom
  } else {
    NavigationPlacement.Side
  }
}
