package me.zhangls.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuite
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import kotlin.math.roundToInt

/**
 * 一个 Tab 的展示信息。顺序即导航套件里的顺序。
 */
private enum class MainTab(val destination: TabDestination, val label: StringResource, val icon: ImageVector) {
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
 * 外壳消费导航套件占用的系统内边距，允许内容超出自身布局边界绘制
 * （见 [navigationSuiteInsets]）。内容区因此不需要知道"导航在哪一侧"、
 * 更不需要自己换算"哪几边该少留白" —— 各页面直接使用 `Scaffold` 给出的 padding 即可。
 *
 * @param selected 当前选中的 Tab，由返回栈决定；为 `null` 表示当前不在任何 Tab 内
 *   （如登录页），此时不显示导航套件
 * @param onSelectTab 点击某个 Tab
 * @param navigationProgress 导航可见比例，与全屏页面的缩放进度共用；0 隐藏，1 完全显示
 * @param content 外壳内容，即 `NavDisplay`
 */
@Composable
fun AppShell(
  selected: TabDestination?,
  onSelectTab: (TabDestination) -> Unit,
  modifier: Modifier = Modifier,
  navigationProgress: () -> Float = { 1f },
  content: @Composable () -> Unit,
) {
  // Keep one surface and one content position when entering or leaving a Tab. Moving the
  // NavDisplay subtree between branches conflicts with its own movable entry content.
  val layoutType = if (selected == null) {
    null
  } else {
    // 窗口事实由组合根提供；无导航时不读取或派生第二套窗口策略。
    val adaptiveInfo = LocalWindowAdaptiveInfo.current
    remember(adaptiveInfo) { adaptiveInfo.navigationSuiteType() }
  }
  val navigationVisible by remember(navigationProgress) { derivedStateOf { navigationProgress() > 0f } }
  val visible = layoutType != null && navigationVisible
  val measurePolicy = remember(layoutType, navigationProgress) {
    navigationMeasurePolicy(layoutType, navigationProgress)
  }

  Surface(
    modifier = modifier,
    color = if (layoutType == null) Color.Transparent else NavigationSuiteScaffoldDefaults.containerColor,
    contentColor = if (layoutType == null) LocalContentColor.current else NavigationSuiteScaffoldDefaults.contentColor,
  ) {
    // The library scaffold starts its own animation. Reuse its components and measurement
    // contract here, placing navigation directly from the page's current progress instead.
    Layout(
      content = {
        if (layoutType != null) {
          Box {
            NavigationSuite(layoutType = layoutType) {
              MainTab.entries.forEach { tab ->
                item(
                  modifier = if (visible) Modifier else Modifier.clearAndSetSemantics {},
                  icon = { Icon(imageVector = tab.icon, contentDescription = stringResource(tab.label)) },
                  label = { Text(stringResource(tab.label)) },
                  selected = tab.destination == selected,
                  onClick = { onSelectTab(tab.destination) },
                )
              }
            }
          }
        }
        val contentModifier = if (layoutType == null) {
          Modifier
        } else {
          Modifier.consumeWindowInsets(if (visible) layoutType.navigationSuiteInsets() else WindowInsets(0, 0, 0, 0))
        }
        Box(contentModifier, propagateMinConstraints = layoutType == null) {
          content()
        }
      },
      measurePolicy = measurePolicy,
    )
  }
}

private fun navigationMeasurePolicy(layoutType: NavigationSuiteType?, navigationProgress: () -> Float): MeasurePolicy =
  MeasurePolicy { measurables, constraints ->
    val isBar = layoutType == NavigationSuiteType.ShortNavigationBarCompact ||
      layoutType == NavigationSuiteType.ShortNavigationBarMedium
    if (layoutType == null) {
      val body = measurables.single().measure(constraints)
      layout(body.width, body.height) { body.placeRelative(0, 0) }
    } else {
      val navigation = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
      val fraction = navigationProgress().coerceIn(0f, 1f)
      val occupied = ((if (isBar) navigation.height else navigation.width) * fraction).roundToInt()
      val bodyConstraints = if (isBar) {
        val height = constraints.maxHeight - occupied
        constraints.copy(minHeight = height, maxHeight = height)
      } else {
        val width = constraints.maxWidth - occupied
        constraints.copy(minWidth = width, maxWidth = width)
      }
      val body = measurables[1].measure(bodyConstraints)
      layout(constraints.maxWidth, constraints.maxHeight) {
        if (isBar) {
          body.placeRelative(0, 0)
          navigation.placeRelative(0, constraints.maxHeight - occupied)
        } else {
          body.placeRelative(occupied, 0)
          navigation.placeRelative(occupied - navigation.width, 0)
        }
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
 * 导航有可见占位时，外壳消费导航所在侧的系统 Insets，避免页面 Scaffold 重复留白。
 * 完全隐藏时不消费，让底层页面使用完整窗口的安全区。方向由 WindowInsetsSides
 * 表达，侧栏在 RTL 下移动到右侧时仍消费实际的 Start 边。
 */
@Composable
private fun NavigationSuiteType.navigationSuiteInsets(): WindowInsets = when (this) {
  NavigationSuiteType.ShortNavigationBarCompact,
  NavigationSuiteType.ShortNavigationBarMedium,
  -> ShortNavigationBarDefaults.windowInsets.only(WindowInsetsSides.Bottom)

  NavigationSuiteType.WideNavigationRailCollapsed,
  NavigationSuiteType.WideNavigationRailExpanded,
  -> WideNavigationRailDefaults.windowInsets.only(WindowInsetsSides.Start)

  // 当前导航策略只选择上面的 ShortNavigationBar / WideNavigationRail 形态。
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
