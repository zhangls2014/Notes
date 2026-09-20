package me.zhangls.email.home

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldPaneScope
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal actual fun PaneScaffold(
  scaffoldNavigator: ThreePaneScaffoldNavigator<Long>,
  listPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
  detailPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
) {
  NavigableListDetailPaneScaffold(
    navigator = scaffoldNavigator,
    defaultBackBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange,
    listPane = listPane,
    detailPane = detailPane,
  )
}
