package me.zhangls.email.home

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldPaneScope
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable

/**
 * 列表-详情双栏骨架的平台差异。
 *
 * - Android：`NavigableListDetailPaneScaffold`（内置返回行为）
 * - iOS：`ListDetailPaneScaffold` + 自行计算的 directive（iPad 分屏下才自适应）
 *
 * 这份差异原先是用**两份完整的 HomeScreen 实现**表达的：两个平台文件里 listPane / detailPane
 * 的装配逐行同构，只有外层 Scaffold 不同 —— 于是每加一个 pane 参数都要同步改两个文件，
 * 漏改一侧不会有编译期提示。现在差异只剩这一层包裹，装配写在 [HomeScreen] 里一次。
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal expect fun PaneScaffold(
  scaffoldNavigator: ThreePaneScaffoldNavigator<Long>,
  listPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
  detailPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
)
