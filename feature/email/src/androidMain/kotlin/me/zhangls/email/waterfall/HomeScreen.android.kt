package me.zhangls.email.waterfall

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
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

  NavigableListDetailPaneScaffold(
    navigator = scaffoldNavigator,
    defaultBackBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange,
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
