package me.zhangls.email.home

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldPaneScope
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal actual fun PaneScaffold(
  scaffoldNavigator: ThreePaneScaffoldNavigator<Long>,
  listPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
  detailPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
) {
  // 按当前窗口自适应信息计算 directive，避免 iPad 分屏下不自适应
  val directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())

  ListDetailPaneScaffold(
    directive = directive,
    scaffoldState = scaffoldNavigator.scaffoldState,
    listPane = listPane,
    detailPane = detailPane,
  )
}
