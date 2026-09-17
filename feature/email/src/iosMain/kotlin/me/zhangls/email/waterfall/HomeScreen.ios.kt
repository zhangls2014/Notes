package me.zhangls.email.waterfall

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
actual fun HomeScreen(isBottomNavigationBar: Boolean) {
  val viewModel: EmailViewModel = koinViewModel()
  val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Long>()
  val scope = rememberCoroutineScope()
  // 按当前窗口自适应信息计算 directive，避免 iPad 分屏下不自适应
  val directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())

  ListDetailPaneScaffold(
    directive = directive,
    scaffoldState = scaffoldNavigator.scaffoldState,
    listPane = {
      HomeListPane(
        viewModel = viewModel,
        isBottomNavigationBar = isBottomNavigationBar,
        scaffoldNavigator = scaffoldNavigator,
        navigateToDetail = scope.navigateToDetailOf(scaffoldNavigator),
      )
    },
    detailPane = {
      HomeDetailPane(
        viewModel = viewModel,
        isBottomNavigationBar = isBottomNavigationBar,
        scaffoldNavigator = scaffoldNavigator,
        scope = scope,
      )
    }
  )
}
