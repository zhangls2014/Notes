package me.zhangls.email.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldPaneScope
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.zhangls.email.component.EmailDetail
import me.zhangls.email.mvi.EmailViewModel
import me.zhangls.email.component.EmailList

@OptIn(
  ExperimentalMaterial3AdaptiveApi::class,
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
expect fun HomeScreen(isBottomNavigationBar: Boolean)

/**
 * 两平台 HomeScreen 共享的导航与内容装配：
 * 平台 actual 只负责选择 Scaffold（Android: Navigable / iOS: 普通 + 自适应 directive）。
 */

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun CoroutineScope.navigateToDetailOf(
  scaffoldNavigator: ThreePaneScaffoldNavigator<Long>
): (Long) -> Unit = {
  launch { scaffoldNavigator.navigateTo(pane = ListDetailPaneScaffoldRole.Detail, contentKey = it) }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun ThreePaneScaffoldPaneScope.HomeListPane(
  viewModel: EmailViewModel,
  isBottomNavigationBar: Boolean,
  scaffoldNavigator: ThreePaneScaffoldNavigator<Long>,
  navigateToDetail: (Long) -> Unit,
) {
  AnimatedPane {
    EmailList(
      viewModel = viewModel,
      isFavorite = false,
      isBottomNavigationBar = isBottomNavigationBar,
      openedEmailId = scaffoldNavigator.currentDestination?.contentKey,
      navigateToDetail = navigateToDetail
    )
  }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun ThreePaneScaffoldPaneScope.HomeDetailPane(
  viewModel: EmailViewModel,
  isBottomNavigationBar: Boolean,
  scaffoldNavigator: ThreePaneScaffoldNavigator<Long>,
  scope: CoroutineScope,
) {
  val emailId = scaffoldNavigator.currentDestination?.contentKey
  val showBack = scaffoldNavigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden
  val onBackPressed: (() -> Unit)? = if (showBack) {
    {
      scope.launch { scaffoldNavigator.navigateBack() }
    }
  } else null

  if (emailId != null) {
    AnimatedPane {
      EmailDetail(
        emailId = emailId,
        isStandalone = false,
        isBottomNavigationBar = isBottomNavigationBar,
        viewModel = viewModel,
        onBackPressed = onBackPressed
      )
    }
  }
}
