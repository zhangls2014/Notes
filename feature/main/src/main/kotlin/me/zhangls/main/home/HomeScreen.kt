package me.zhangls.main.home

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeScreen(viewmodel: HomeViewModel = hiltViewModel(), isBottomNavigationBar: Boolean) {
  val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Long>()
  val scope = rememberCoroutineScope()

  NavigableListDetailPaneScaffold(
    navigator = scaffoldNavigator,
    defaultBackBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange,
    listPane = {
      EmailList(
        isBottomNavigationBar = isBottomNavigationBar,
        viewmodel = viewmodel,
        openedEmailId = scaffoldNavigator.currentDestination?.contentKey,
        navigateToDetail = {
          scope.launch {
            scaffoldNavigator.navigateTo(pane = ListDetailPaneScaffoldRole.Detail, contentKey = it)
          }
        },
      )
    },
    detailPane = {
      scaffoldNavigator.currentDestination?.contentKey?.let {
        EmailDetail(isBottomNavigationBar = isBottomNavigationBar, emailId = it, viewmodel = viewmodel) {
          scope.launch {
            if (scaffoldNavigator.canNavigateBack()) {
              scaffoldNavigator.navigateBack()
            }
          }
        }
      }
    }
  )
}

@Preview(showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
  HomeScreen(isBottomNavigationBar = false)
}