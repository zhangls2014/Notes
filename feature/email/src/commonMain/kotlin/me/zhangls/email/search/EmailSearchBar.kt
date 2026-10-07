package me.zhangls.email.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldState
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
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.zhangls.data.model.EmailModel
import me.zhangls.theme.component.UserAvatar
import me.zhangls.email.component.ProfileImage
import me.zhangls.email.search.SearchViewModel.Companion.DURATION_SEARCH_DEBOUNCE
import me.zhangls.theme.component.TooltipIconButton
import me.zhangls.theme.icon.ArrowBackIosNew
import me.zhangls.theme.icon.Clear
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Search
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo
import me.zhangls.theme.layout.isCompactWidth
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_action_delete
import notes.feature.email.generated.resources.email_action_owner_info
import notes.feature.email.generated.resources.email_action_search_collapsed
import notes.feature.email.generated.resources.email_hint_search
import notes.feature.email.generated.resources.email_msg_no_item_found
import notes.feature.email.generated.resources.email_msg_no_search_history
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * @author zhangls
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
internal fun EmailSearchBar(
  scrollBehavior: SearchBarScrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(),
  onResultClick: (Long) -> Unit = {},
  onProfileClick: () -> Unit = {},
) {
  val viewModel: SearchViewModel = koinViewModel()
  val state by viewModel.state.collectAsStateWithLifecycle()
  val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
  // 形态由**窗口宽度**决定：要不要摊开一个 docked 结果面板，问的是"有没有横向空间"。
  // 原先这里读的是 `LocalNavigationPlacement == Bottom`（注释还写着"紧凑窗口"）——
  // 那是宿主布局的**推导结果**，只是碰巧与宽度相关；导航套件的形态策略一变，
  // 搜索栏的形态就会跟着莫名改变。契约里缺的是"窗口多大"这个事实，不是"导航在哪一侧"。
  val useFullScreenSearchBar = LocalWindowAdaptiveInfo.current.isCompactWidth
  val density = LocalDensity.current
  var collapsedInputWidth by remember { mutableIntStateOf(0) }

  val textFieldState = rememberTextFieldState(initialText = state.searchText)
  val initialSearchBarValue = remember { state.searchBarValue }
  val searchBarState = rememberSearchBarState(initialValue = initialSearchBarValue)
  val scope = rememberCoroutineScope()
  val inputModeManager = LocalInputModeManager.current

  val clearSearchQuery: () -> Unit = {
    textFieldState.clearText()
    viewModel.sendIntent(SearchIntent.UpdateSearchText(""))
  }
  val closeSearchBar: () -> Unit = {
    clearSearchQuery()
    scope.launch { searchBarState.animateToCollapsed() }
  }

  val searchPlaceholder = stringResource(Res.string.email_hint_search)
  val inputField = @Composable { modifier: Modifier, measuring: Boolean ->
    SearchBarDefaults.InputField(
      searchBarState = if (measuring) rememberSearchBarState() else searchBarState,
      textFieldState = if (measuring) remember { TextFieldState() } else textFieldState,
      enabled = !measuring,
      readOnly = measuring || searchBarState.currentValue == SearchBarValue.Collapsed,
      // 系统预测返回可能把焦点交给重新出现的输入框；触摸模式下这会自动展开搜索。
      // 收起时由 InputField 的点击处理启动展开，键盘模式仍允许 Tab 聚焦。
      modifier = modifier.focusProperties {
        canFocus = !measuring && (searchBarState.currentValue == SearchBarValue.Expanded ||
          inputModeManager.inputMode != InputMode.Touch)
      },
      onSearch = { closeSearchBar() },
      placeholder = {
        Text(
          text = searchPlaceholder,
          modifier = Modifier.clearAndSetSemantics {}
        )
      },
      leadingIcon = {
        if (!measuring && searchBarState.currentValue == SearchBarValue.Expanded) {
          // 返回键复用通用形态：图标 + 提示气泡，提示位置在 core:theme 里定
          TooltipIconButton(
            icon = Icons.Rounded.ArrowBackIosNew,
            label = stringResource(Res.string.email_action_search_collapsed),
            onClick = closeSearchBar,
          )
        } else {
          Icon(Icons.Rounded.Search, contentDescription = null)
        }
      },
      trailingIcon = {
        state.user?.also {
          val profileLabel = stringResource(Res.string.email_action_owner_info)
          IconButton(
            modifier = Modifier.size(48.dp).semantics { contentDescription = profileLabel },
            enabled = !measuring,
            onClick = {
              scope.launch {
                searchBarState.animateToCollapsed()
                clearSearchQuery()
                viewModel.sendIntent(SearchIntent.UpdateSearchBarValue(SearchBarValue.Collapsed))
                onProfileClick()
              }
            },
          ) {
            UserAvatar(
              path = it.avatar,
              contentDescription = null,
              modifier = Modifier.size(40.dp),
            )
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
          viewModel.sendIntent(SearchIntent.SelectSearchHistory(it))
        },
        onHistoryDelete = {
          viewModel.sendIntent(SearchIntent.DeleteSearchHistory(it))
        },
      )
    } else {
      SearchResults(searchResults = searchResults) {
        val searchText = textFieldState.text.toString()
        viewModel.sendIntent(SearchIntent.UpdateSearchText(searchText))
        viewModel.sendIntent(SearchIntent.SaveSearchHistory(searchText))
        scope.launch {
          // 单窗格导航会移除列表组合并取消此作用域。先收起弹层，避免 Entry
          // 保存未完成的动画进度，返回或旋转后恢复成悬浮的搜索窗口。
          searchBarState.animateToCollapsed()
          clearSearchQuery()
          viewModel.sendIntent(SearchIntent.UpdateSearchBarValue(SearchBarValue.Collapsed))
          onResultClick(it)
        }
      }
    }
  }

  // 展开状态回写只用于 ViewModel 快照，不能再作为变化中的 rememberSaveable input。
  // 否则动画途中会重建 SearchBarState，丢失布局坐标，并让点击回调和聚焦逻辑
  // 持有不同的状态对象。初始值只读取一次；重建页面时由 rememberSearchBarState 恢复。
  LaunchedEffect(searchBarState.currentValue) {
    viewModel.sendIntent(SearchIntent.UpdateSearchBarValue(searchBarState.currentValue))
    if (searchBarState.currentValue == SearchBarValue.Collapsed) {
      // 只清理查询，不再次启动动画；重复收起会取消结果点击正在等待的动画。
      clearSearchQuery()
    }
  }

  LaunchedEffect(viewModel) {
    snapshotFlow { textFieldState.text.toString() }
      .debounce(DURATION_SEARCH_DEBOUNCE)
      .distinctUntilChanged()
      .collect {
        viewModel.sendIntent(SearchIntent.UpdateSearchText(it))
      }
  }

  AppBarWithSearch(
    scrollBehavior = scrollBehavior,
    state = searchBarState,
    inputField = {
      MeasuredSearchInput(
        measurementKey = searchPlaceholder,
        modifier = Modifier.onSizeChanged { if (it.width > 0) collapsedInputWidth = it.width },
      ) { measuring ->
        inputField(Modifier, measuring)
      }
    },
    colors = SearchBarDefaults.appBarWithSearchColors(
      scrolledSearchBarContainerColor = Color.Unspecified,
      appBarContainerColor = Color.Unspecified,
      scrolledAppBarContainerColor = Color.Unspecified,
    ),
  )

  if (useFullScreenSearchBar) {
    ExpandedFullScreenSearchBar(
      state = searchBarState,
      // 全屏输入跟随 Material 的展开动画宽度。
      inputField = { inputField(Modifier.fillMaxWidth(), false) },
      content = outputField
    )
  } else {
    ExpandedDockedSearchBar(
      state = searchBarState,
      // LayoutCoordinates changes do not independently invalidate the popup's measurement.
      // Observe the placed anchor width so the final resize frame also remeasures the popup.
      modifier = if (collapsedInputWidth > 0) {
        Modifier.width(with(density) { collapsedInputWidth.toDp() })
      } else Modifier,
      properties = dockedSearchPopupProperties(),
      // 只填满 Material 从收起态锚点读取的宽度。
      inputField = { inputField(Modifier.fillMaxWidth(), false) },
      content = outputField
    )
  }
}

@Composable
private fun SearchResults(
  searchResults: LazyPagingItems<EmailModel>,
  onResultClick: (Long) -> Unit,
) {
  if (searchResults.itemCount <= 0) {
    Text(
      text = stringResource(Res.string.email_msg_no_item_found),
      modifier = Modifier.padding(16.dp),
    )
    return
  }

  LazyColumn(modifier = Modifier.fillMaxWidth()) {
    items(count = searchResults.itemCount, key = searchResults.itemKey { it.id }) {
      val email = searchResults[it] ?: return@items
      val sender = email.sender
      ListItem(
        headlineContent = { Text(email.subject) },
        supportingContent = { Text(sender.fullName) },
        leadingContent = {
          ProfileImage(
            drawableKey = sender.avatar,
            description = null
          )
        },
        modifier = Modifier.clickable { onResultClick(email.id) },
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
      text = stringResource(Res.string.email_msg_no_search_history),
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
              contentDescription = stringResource(Res.string.email_action_delete),
              modifier = Modifier.size(AssistChipDefaults.IconSize),
            )
          }
        }
      )
    }
  }
}
