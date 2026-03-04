package me.zhangls.main.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import me.zhangls.main.EmailIntent
import me.zhangls.main.EmailViewModel
import me.zhangls.main.R
import me.zhangls.main.compose.ActionItem
import me.zhangls.main.compose.EmailActionBar
import me.zhangls.main.compose.EmailDetail
import me.zhangls.main.compose.EmailList
import me.zhangls.main.compose.EmailSearchBar

@OptIn(
  ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class,
  ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun HomeScreen(viewmodel: EmailViewModel, isBottomNavigationBar: Boolean) {
  val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<Long>()
  val scope = rememberCoroutineScope()
  val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
  val emailListState = rememberLazyListState()
  val navigateToDetail: (Long) -> Unit = {
    scope.launch { scaffoldNavigator.navigateTo(pane = ListDetailPaneScaffoldRole.Detail, contentKey = it) }
  }
  val emailItems = viewmodel.emailPaging.collectAsLazyPagingItems()
  val state by viewmodel.state.collectAsStateWithLifecycle()
  val items = listOf(
    ActionItem(Icons.Rounded.Star, R.string.main_action_favorite),
    ActionItem(Icons.Rounded.StarOutline, R.string.main_action_cancel_favorite),
    ActionItem(Icons.Rounded.Delete, R.string.main_action_delete),
    ActionItem(Icons.Rounded.Cancel, R.string.main_action_cancel),
  )

  NavigableListDetailPaneScaffold(
    navigator = scaffoldNavigator,
    defaultBackBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange,
    listPane = {
      Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            if (state.selectedItems.isEmpty()) {
              EmailSearchBar(
                isBottomNavigationBar = isBottomNavigationBar,
                scrollBehavior = scrollBehavior,
                viewmodel = viewmodel,
                onResultClick = navigateToDetail
              )
            } else {
              EmailActionBar(modifier = Modifier.statusBarsPadding(), items = items) {
                when (it.text) {
                  R.string.main_action_favorite -> {
                    viewmodel.handleIntent(EmailIntent.MultiFavorite)
                  }

                  R.string.main_action_cancel_favorite -> {
                    viewmodel.handleIntent(EmailIntent.MultiCancelFavorite)
                  }

                  R.string.main_action_delete -> {
                    viewmodel.handleIntent(EmailIntent.MultiDelete)
                  }

                  R.string.main_action_cancel -> {
                    viewmodel.handleIntent(EmailIntent.ClearSelectedEmail)
                  }
                }
              }
            }
          }
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
              expanded = emailListState.lastScrolledBackward || emailListState.canScrollBackward.not(),
            )
          }
        },
      ) {
        EmailList(
          contentPadding = it,
          emailListState = emailListState,
          emailItems = emailItems,
          viewmodel = viewmodel,
          openedEmailId = scaffoldNavigator.currentDestination?.contentKey,
          navigateToDetail = navigateToDetail
        )
      }
    },
    detailPane = {
      scaffoldNavigator.currentDestination?.contentKey?.let {
        EmailDetail(
          emailId = it, viewmodel = viewmodel, if (isBottomNavigationBar) {
            { scope.launch { scaffoldNavigator.navigateBack() } }
          } else {
            null
          })
      }
    }
  )
}