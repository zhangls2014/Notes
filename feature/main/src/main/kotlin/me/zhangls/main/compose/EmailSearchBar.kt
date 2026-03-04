package me.zhangls.main.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.launch
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.model.toDomain
import me.zhangls.main.EmailIntent
import me.zhangls.main.EmailViewModel
import me.zhangls.main.R

/**
 * @author zhangls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmailSearchBar(
  isBottomNavigationBar: Boolean,
  scrollBehavior: SearchBarScrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
  viewmodel: EmailViewModel,
  onResultClick: (Long) -> Unit = {}
) {
  val state by viewmodel.state.collectAsStateWithLifecycle()
  val ownerAccount = state.ownerAccount
  val searchResults = viewmodel.searchResults.collectAsLazyPagingItems()

  val textFieldState = rememberTextFieldState()
  val searchBarState = rememberSearchBarState()
  val scope = rememberCoroutineScope()
  val inputField = @Composable {
    SearchBarDefaults.InputField(
      searchBarState = searchBarState,
      textFieldState = textFieldState,
      onSearch = {
        textFieldState.clearText()
        scope.launch { searchBarState.animateToCollapsed() }
      },
      placeholder = {
        Text(
          text = stringResource(R.string.main_hint_search),
          modifier = Modifier.clearAndSetSemantics {}
        )
      },
      leadingIcon = {
        if (searchBarState.currentValue == SearchBarValue.Expanded) {
          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(stringResource(R.string.main_action_search_collapsed)) } },
            state = rememberTooltipState(),
          ) {
            IconButton(onClick = { scope.launch { searchBarState.animateToCollapsed() } }) {
              Icon(
                imageVector = Icons.Rounded.ArrowBackIosNew,
                contentDescription = stringResource(R.string.main_action_search_collapsed),
              )
            }
          }
        } else {
          Icon(Icons.Default.Search, contentDescription = null)
        }
      },
      trailingIcon = {
        if (ownerAccount == null) {
          Icon(Icons.Default.MoreVert, contentDescription = null)
        } else {
          ProfileImage(
            drawableResource = ownerAccount.avatar,
            description = stringResource(id = R.string.main_action_owner_info),
          )
        }
      },
    )
  }

  LaunchedEffect(textFieldState.text) {
    viewmodel.handleIntent(EmailIntent.UpdateSearchText(textFieldState.text))
  }

  TopSearchBar(
    scrollBehavior = scrollBehavior,
    state = searchBarState,
    inputField = inputField,
  )

  if (isBottomNavigationBar) {
    ExpandedFullScreenSearchBar(state = searchBarState, inputField = inputField) {
      SearchResults(searchText = textFieldState.text, searchResults = searchResults) {
        textFieldState.clearText()
        scope.launch { searchBarState.animateToCollapsed() }
        onResultClick(it)
      }
    }
  } else {
    ExpandedDockedSearchBar(state = searchBarState, inputField = inputField) {
      SearchResults(searchText = textFieldState.text, searchResults = searchResults) {
        textFieldState.clearText()
        scope.launch { searchBarState.animateToCollapsed() }
        onResultClick(it)
      }
    }
  }
}


@Composable
private fun SearchResults(
  searchText: CharSequence,
  searchResults: LazyPagingItems<EmailConvertModel>,
  onResultClick: (Long) -> Unit,
) {
  if (searchResults.itemCount > 0) {
    LazyColumn(modifier = Modifier.fillMaxWidth()) {
      items(count = searchResults.itemCount, key = { searchResults[it]!!.email.id }) {
        val email = searchResults[it] ?: return@items
        val sender = email.sender.toDomain()
        ListItem(
          headlineContent = { Text(email.email.subject) },
          supportingContent = { Text(sender.fullName) },
          leadingContent = {
            ProfileImage(
              drawableResource = sender.avatar,
              description = stringResource(id = R.string.main_action_owner_info),
            )
          },
          modifier = Modifier.clickable { onResultClick(email.email.id) },
        )
      }
    }
  } else if (searchText.isNotEmpty()) {
    Text(
      text = stringResource(id = R.string.main_msg_no_item_found),
      modifier = Modifier.padding(16.dp),
    )
  } else {
    Text(
      text = stringResource(id = R.string.main_msg_no_search_history),
      modifier = Modifier.padding(16.dp),
    )
  }
}