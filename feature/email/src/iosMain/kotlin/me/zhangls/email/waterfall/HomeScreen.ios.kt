package me.zhangls.email.waterfall

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import me.zhangls.email.component.EmailDetail
import me.zhangls.email.component.EmailList
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
actual fun HomeScreen(isBottomNavigationBar: Boolean) {
  val viewmodel: EmailViewModel = koinViewModel()
  val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Long>()
  val scope = rememberCoroutineScope()
  val navigateToDetail: (Long) -> Unit = {
    scope.launch { scaffoldNavigator.navigateTo(pane = ListDetailPaneScaffoldRole.Detail, contentKey = it) }
  }
  val scaffoldValue = scaffoldNavigator.scaffoldValue
  val showBack = scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden

  ListDetailPaneScaffold(
    directive = PaneScaffoldDirective.Default,
    scaffoldState = scaffoldNavigator.scaffoldState,
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
