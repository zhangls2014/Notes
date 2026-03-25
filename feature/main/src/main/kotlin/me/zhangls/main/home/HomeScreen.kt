package me.zhangls.main.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import me.zhangls.main.EmailViewModel
import me.zhangls.main.compose.EmailDetail
import me.zhangls.main.compose.EmailList

@OptIn(
  ExperimentalMaterial3AdaptiveApi::class,
  ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun HomeScreen(isBottomNavigationBar: Boolean, viewmodel: EmailViewModel) {
  val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Long>()
  val scope = rememberCoroutineScope()
  val navigateToDetail: (Long) -> Unit = {
    scope.launch { scaffoldNavigator.navigateTo(pane = ListDetailPaneScaffoldRole.Detail, contentKey = it) }
  }
  val scaffoldValue = scaffoldNavigator.scaffoldValue
  val showBack = scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden

  NavigableListDetailPaneScaffold(
    navigator = scaffoldNavigator,
    defaultBackBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange,
    listPane = {
      AnimatedPane {
        EmailList(
          viewmodel = viewmodel,
          isFavorite = false,
          isBottomNavigationBar = isBottomNavigationBar,
          openedEmailId = scaffoldNavigator.currentDestination?.contentKey,
          navigateToDetail = navigateToDetail
        )
      }
    },
    detailPane = {
      val emailId = scaffoldNavigator.currentDestination?.contentKey
      val onBackPressed: (() -> Unit)? = if (showBack) {
        {
          scope.launch { scaffoldNavigator.navigateBack() }
        }
      } else null

      if (emailId != null) {
        AnimatedPane {
          EmailDetail(
            emailId = emailId,
            isFavorite = false,
            isBottomNavigationBar = isBottomNavigationBar,
            viewmodel = viewmodel,
            onBackPressed = onBackPressed
          )
        }
      }
    }
  )
}
