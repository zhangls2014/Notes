package me.zhangls.email.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.mohamedrejeb.calf.io.KmpFile
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.model.UserModel
import me.zhangls.data.model.toDomain
import me.zhangls.email.component.ProfileImage
import me.zhangls.email.icon.Clear
import me.zhangls.email.icon.Search
import me.zhangls.email.search.SearchViewModel.Companion.DURATION_SEARCH_DEBOUNCE
import me.zhangls.theme.icon.ArrowBackIosNew
import me.zhangls.theme.icon.Icons
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.main_action_delete
import notes.feature.email.generated.resources.main_action_owner_info
import notes.feature.email.generated.resources.main_action_search_collapsed
import notes.feature.email.generated.resources.main_hint_search
import notes.feature.email.generated.resources.main_ic_default_avatar
import notes.feature.email.generated.resources.main_msg_no_item_found
import notes.feature.email.generated.resources.main_msg_no_search_history
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * @author zhangls
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
internal fun EmailSearchBar(
  isBottomNavigationBar: Boolean,
  scrollBehavior: SearchBarScrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
  onResultClick: (Long) -> Unit = {}
) {
  val viewmodel: SearchViewModel = koinViewModel()
  val state by viewmodel.state.collectAsStateWithLifecycle()
  val searchResults = viewmodel.searchResults.collectAsLazyPagingItems()

  val textFieldState = rememberTextFieldState(initialText = state.searchText)
  val searchBarState = rememberSearchBarState(initialValue = state.searchBarValue)
  val scope = rememberCoroutineScope()
  // 是否是第一次渲染
  var initial by remember { mutableStateOf(true) }

  val closeSearchBar: () -> Unit = {
    textFieldState.clearText()
    viewmodel.sendIntent(SearchIntent.UpdateSearchText(""))
    scope.launch { searchBarState.animateToCollapsed() }
  }

  val inputField = @Composable {
    SearchBarDefaults.InputField(
      searchBarState = searchBarState,
      textFieldState = textFieldState,
      readOnly = searchBarState.currentValue == SearchBarValue.Collapsed,
      onSearch = { closeSearchBar() },
      placeholder = {
        Text(
          text = stringResource(Res.string.main_hint_search),
          modifier = Modifier.clearAndSetSemantics {}
        )
      },
      leadingIcon = {
        if (searchBarState.currentValue == SearchBarValue.Expanded) {
          TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            tooltip = { PlainTooltip { Text(stringResource(Res.string.main_action_search_collapsed)) } },
            state = rememberTooltipState(),
          ) {
            IconButton(onClick = closeSearchBar) {
              Icon(
                imageVector = Icons.Rounded.ArrowBackIosNew,
                contentDescription = stringResource(Res.string.main_action_search_collapsed),
              )
            }
          }
        } else {
          Icon(Icons.Rounded.Search, contentDescription = null)
        }
      },
      trailingIcon = {
        state.user?.also {
          ProfileImage(it) { kmpFile ->
            viewmodel.sendIntent(SearchIntent.UpdateSelectedAvatar(kmpFile))
          }
        }
      },
    )
  }
  val outputField = @Composable { columnScope: ColumnScope ->
    val searchText = textFieldState.text

    if (searchText.isEmpty()) {
      SearchHistory(
        searchHistory = state.searchHistory,
        onHistoryClick = {
          textFieldState.edit { replace(0, length, it) }
          viewmodel.sendIntent(SearchIntent.SelectSearchHistory(it))
        },
        onHistoryDelete = {
          viewmodel.sendIntent(SearchIntent.DeleteSearchHistory(it))
        })
    } else {
      SearchResults(searchResults = searchResults) {
        val searchText = textFieldState.text.toString()
        viewmodel.sendIntent(SearchIntent.UpdateSearchText(searchText))
        viewmodel.sendIntent(SearchIntent.SaveSearchHistory(searchText))
        closeSearchBar()
        onResultClick(it)
      }
    }
  }

  /**
   * 为了适配不同屏幕，采用了 [ExpandedFullScreenSearchBar] 和 [ExpandedDockedSearchBar] 来渲染搜索框。
   * 在屏幕旋转时，它们的状态时不同步的，所以这里需要手动同步一下状态
   */
  LaunchedEffect(searchBarState.currentValue) {
    // 如果第一次渲染且当前值与保留值不一致，则不更新
    if (initial) {
      if (state.searchText != textFieldState.text) {
        textFieldState.edit {
          replace(0, length, state.searchText)
        }
      }

      if (state.searchBarValue != searchBarState.currentValue) {
        if (state.searchBarValue == SearchBarValue.Expanded) {
          searchBarState.snapTo(1F)
        } else {
          searchBarState.snapTo(0F)
        }
      } else if (searchBarState.currentValue == SearchBarValue.Collapsed) {
        scope.launch { searchBarState.animateToCollapsed() }
      }
      initial = false
      return@LaunchedEffect
    }
    viewmodel.sendIntent(SearchIntent.UpdateSearchBarValue(searchBarState.currentValue))
    if (searchBarState.currentValue == SearchBarValue.Collapsed) {
      closeSearchBar()
    }
  }

  LaunchedEffect(viewmodel) {
    snapshotFlow { textFieldState.text.toString() }
      .debounce(DURATION_SEARCH_DEBOUNCE)
      .distinctUntilChanged()
      .collect {
        viewmodel.sendIntent(SearchIntent.UpdateSearchText(it))
      }
  }

  AppBarWithSearch(
    scrollBehavior = scrollBehavior,
    state = searchBarState,
    inputField = inputField,
    colors = SearchBarDefaults.appBarWithSearchColors(
      scrolledSearchBarContainerColor = Color.Unspecified,
      appBarContainerColor = Color.Unspecified,
      scrolledAppBarContainerColor = Color.Unspecified,
    ),
  )

  if (isBottomNavigationBar) {
    ExpandedFullScreenSearchBar(
      state = searchBarState,
      inputField = inputField,
      content = outputField
    )
  } else {
    ExpandedDockedSearchBar(
      state = searchBarState,
      inputField = inputField,
      content = outputField
    )
  }
}

@Composable
private fun ProfileImage(user: UserModel, onImageSelected: (KmpFile?) -> Unit) {
  val launcher = rememberFilePickerLauncher(
    type = FilePickerFileType.Image,
    selectionMode = FilePickerSelectionMode.Single,
    onResult = { onImageSelected(it.firstOrNull()) }
  )

  AsyncImage(
    modifier = Modifier
      .size(40.dp)
      .clip(CircleShape)
      .clickable { launcher.launch() },
    model = user.avatar,
    placeholder = painterResource(Res.drawable.main_ic_default_avatar),
    error = painterResource(Res.drawable.main_ic_default_avatar),
    contentScale = ContentScale.Crop,
    contentDescription = stringResource(Res.string.main_action_owner_info),
  )
}


@Composable
private fun SearchResults(
  searchResults: LazyPagingItems<EmailConvertModel>,
  onResultClick: (Long) -> Unit,
) {
  if (searchResults.itemCount <= 0) {
    Text(
      text = stringResource(Res.string.main_msg_no_item_found),
      modifier = Modifier.padding(16.dp),
    )
    return
  }

  LazyColumn(modifier = Modifier.fillMaxWidth()) {
    items(count = searchResults.itemCount, key = searchResults.itemKey { it.email.id }) {
      val email = searchResults[it] ?: return@items
      val sender = email.sender.toDomain()
      ListItem(
        headlineContent = { Text(email.email.subject) },
        supportingContent = { Text(sender.fullName) },
        leadingContent = {
          ProfileImage(
            drawableKey = sender.avatar,
            description = stringResource(Res.string.main_action_owner_info)
          )
        },
        modifier = Modifier.clickable { onResultClick(email.email.id) },
      )
    }
  }
}

@Composable
private fun SearchHistory(
  searchHistory: List<String>,
  onHistoryClick: (String) -> Unit,
  onHistoryDelete: (String) -> Unit,
) {
  if (searchHistory.isEmpty()) {
    Text(
      text = stringResource(Res.string.main_msg_no_search_history),
      modifier = Modifier.padding(16.dp),
    )
    return
  }

  FlowRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    searchHistory.forEach { keyword ->
      AssistChip(
        onClick = { onHistoryClick(keyword) },
        label = { Text(keyword) },
        leadingIcon = {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(AssistChipDefaults.IconSize),
          )
        },
        trailingIcon = {
          IconButton(
            onClick = { onHistoryDelete(keyword) },
            modifier = Modifier.size(AssistChipDefaults.IconSize)
          ) {
            Icon(
              imageVector = Icons.Rounded.Clear,
              contentDescription = stringResource(Res.string.main_action_delete),
              modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
          }
        }
      )
    }
  }
}
