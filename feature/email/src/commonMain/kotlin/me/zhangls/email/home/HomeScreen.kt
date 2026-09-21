package me.zhangls.email.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldPaneScope
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.zhangls.email.component.EmailDetail
import me.zhangls.email.component.EmailList
import me.zhangls.email.mvi.EmailViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * 首页：列表 + 详情双栏。
 *
 * 装配（含两个 pane 的内容）在两平台共享，平台差异只有 [PaneScaffold] 一处。
 */
@OptIn(
  ExperimentalMaterial3AdaptiveApi::class,
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun HomeScreen() {
  // key 与收藏页区分：两个 tab 处于同一个 NavEntry，不区分就会共用同一个实例
  val viewModel: EmailViewModel = koinViewModel(key = EmailViewModel.KEY_HOME)
  val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Long>()
  val scope = rememberCoroutineScope()

  PaneScaffold(
    scaffoldNavigator = scaffoldNavigator,
    listPane = {
      HomeListPane(
        viewModel = viewModel,
        scaffoldNavigator = scaffoldNavigator,
        navigateToDetail = scope.navigateToDetailOf(scaffoldNavigator),
      )
    },
    detailPane = {
      HomeDetailPane(
        viewModel = viewModel,
        scaffoldNavigator = scaffoldNavigator,
        scope = scope,
      )
    },
  )
}

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
  scaffoldNavigator: ThreePaneScaffoldNavigator<Long>,
  navigateToDetail: (Long) -> Unit,
) {
  AnimatedPane {
    EmailList(
      viewModel = viewModel,
      isFavorite = false,
      openedEmailId = scaffoldNavigator.currentDestination?.contentKey,
      navigateToDetail = navigateToDetail
    )
  }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun ThreePaneScaffoldPaneScope.HomeDetailPane(
  viewModel: EmailViewModel,
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
        viewModel = viewModel,
        onBackPressed = onBackPressed
      )
    }
  }
}
