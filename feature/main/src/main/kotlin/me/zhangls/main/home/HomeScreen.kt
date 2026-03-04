package me.zhangls.main.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import me.zhangls.main.EmailViewModel
import me.zhangls.main.R
import me.zhangls.main.compose.EmailDetail
import me.zhangls.main.compose.EmailList
import me.zhangls.main.compose.EmailSearchBar

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeScreen(viewmodel: EmailViewModel, isBottomNavigationBar: Boolean) {
  val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Long>()
  val scope = rememberCoroutineScope()
  val navigateToDetail: (Long) -> Unit = {
    scope.launch {
      scaffoldNavigator.navigateTo(pane = ListDetailPaneScaffoldRole.Detail, contentKey = it)
    }
  }
  val emailItems = viewmodel.emailPaging.collectAsLazyPagingItems()

  NavigableListDetailPaneScaffold(
    navigator = scaffoldNavigator,
    defaultBackBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange,
    listPane = {
      EmailHome(
        isBottomNavigationBar = isBottomNavigationBar,
        viewmodel = viewmodel,
        navigateToDetail = navigateToDetail
      ) {
        EmailList(
          contentPadding = it,
          emailItems = emailItems,
          viewmodel = viewmodel,
          openedEmailId = scaffoldNavigator.currentDestination?.contentKey,
          navigateToDetail = navigateToDetail
        )
      }
    },
    detailPane = {
      scaffoldNavigator.currentDestination?.contentKey?.let {
        EmailDetail(emailId = it, viewmodel = viewmodel, null)
      }
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailHome(
  isBottomNavigationBar: Boolean,
  viewmodel: EmailViewModel,
  navigateToDetail: (Long) -> Unit,
  list: @Composable (PaddingValues) -> Unit
) {
  val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  val emailListState = rememberLazyListState()

  Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      EmailSearchBar(
        isBottomNavigationBar = isBottomNavigationBar,
        scrollBehavior = scrollBehavior,
        viewmodel = viewmodel,
        onResultClick = navigateToDetail
      )
    },
    floatingActionButton = {
      if (isBottomNavigationBar) {
        ExtendedFloatingActionButton(
          text = { Text(text = stringResource(id = R.string.main_action_new_email)) },
          icon = {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = stringResource(id = R.string.main_action_new_email)
            )
          },
          onClick = {},
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          expanded = emailListState.lastScrolledBackward || emailListState.canScrollBackward.not(),
        )
      }
    },
    content = list
  )
}