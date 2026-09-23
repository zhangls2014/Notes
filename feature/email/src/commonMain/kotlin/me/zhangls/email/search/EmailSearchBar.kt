package me.zhangls.email.search

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExpandedDockedSearchBar
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
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
import me.zhangls.email.component.AvatarPicker
import me.zhangls.email.component.ProfileImage
import me.zhangls.email.search.SearchViewModel.Companion.DURATION_SEARCH_DEBOUNCE
import me.zhangls.theme.component.HingeSafeDialog
import me.zhangls.theme.component.TooltipIconButton
import me.zhangls.theme.icon.ArrowBackIosNew
import me.zhangls.theme.icon.Clear
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Search
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo
import me.zhangls.theme.layout.hasHinges
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
  onResultClick: (Long) -> Unit = {}
) {
  val viewModel: SearchViewModel = koinViewModel()
  val state by viewModel.state.collectAsStateWithLifecycle()
  val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
  // 形态由**窗口宽度**决定：要不要摊开一个 docked 结果面板，问的是"有没有横向空间"。
  // 原先这里读的是 `LocalNavigationPlacement == Bottom`（注释还写着"紧凑窗口"）——
  // 那是宿主布局的**推导结果**，只是碰巧与宽度相关；导航套件的形态策略一变，
  // 搜索栏的形态就会跟着莫名改变。契约里缺的是"窗口多大"这个事实，不是"导航在哪一侧"。
  val adaptiveInfo = LocalWindowAdaptiveInfo.current
  val useFullScreenSearchBar = adaptiveInfo.isCompactWidth

  val textFieldState = rememberTextFieldState(initialText = state.searchText)
  val searchBarState = rememberSearchBarState(initialValue = state.searchBarValue)
  val scope = rememberCoroutineScope()
  val historyScrollState = rememberScrollState()
  val resultsScrollState = rememberLazyListState()

  val closeSearchBar: () -> Unit = {
    textFieldState.clearText()
    viewModel.sendIntent(SearchIntent.UpdateSearchText(""))
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
          text = stringResource(Res.string.email_hint_search),
          modifier = Modifier.clearAndSetSemantics {}
        )
      },
      leadingIcon = {
        if (searchBarState.currentValue == SearchBarValue.Expanded) {
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
          AvatarPicker(user = it) { kmpFile ->
            viewModel.sendIntent(SearchIntent.UpdateSelectedAvatar(kmpFile))
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
        scrollState = historyScrollState,
        scrollable = adaptiveInfo.hasHinges,
        onHistoryClick = {
          textFieldState.edit { replace(0, length, it) }
          viewModel.sendIntent(SearchIntent.SelectSearchHistory(it))
        },
        onHistoryDelete = {
          viewModel.sendIntent(SearchIntent.DeleteSearchHistory(it))
        },
      )
    } else {
      SearchResults(searchResults = searchResults, scrollState = resultsScrollState) {
        val searchText = textFieldState.text.toString()
        viewModel.sendIntent(SearchIntent.UpdateSearchText(searchText))
        viewModel.sendIntent(SearchIntent.SaveSearchHistory(searchText))
        closeSearchBar()
        onResultClick(it)
      }
    }
  }

  /**
   * 把搜索栏的展开 / 收起状态回写给 ViewModel。
   *
   * 这里**不再需要**"首帧手工对齐"那段补丁（原先用 `initial` 标志 + `snapTo`：
   * 旋转时 [ExpandedFullScreenSearchBar] 与 [ExpandedDockedSearchBar] 换了一个渲染，
   * 于是强行把值搬过去）。两处状态本来就是框架负责恢复的：
   *
   * - [rememberTextFieldState] 内部是 `rememberSaveable`（无 input），保存值优先；
   * - [rememberSearchBarState] 内部是 `rememberSaveable(initialValue, ...)`，
   *   以 `initialValue` 为 input —— 于是 ViewModel 的值变化时它会自动重置，
   *   不需要手工 `snapTo`。
   *
   * 原先那段补丁还有一处反向伤害：它把输入框内容**覆盖成 ViewModel 里的旧值**
   * （框内文本经 300ms 防抖才回写，旋转若落在防抖窗口内，VM 比用户输入旧）——
   * 也就是说它会吃掉用户刚敲的字。
   */
  LaunchedEffect(searchBarState.currentValue, searchBarState.targetValue, adaptiveInfo.hasHinges) {
    viewModel.sendIntent(SearchIntent.UpdateSearchBarValue(searchBarState.currentValue))
    if (searchBarState.currentValue == SearchBarValue.Collapsed) {
      if (adaptiveInfo.hasHinges) {
        // The first hinge-dialog expansion frame still reports Collapsed as the current value.
        if (searchBarState.targetValue == SearchBarValue.Collapsed) {
          textFieldState.clearText()
          viewModel.sendIntent(SearchIntent.UpdateSearchText(""))
        }
      } else {
        // Preserve the original Material search-bar behavior on ordinary windows.
        closeSearchBar()
      }
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
    inputField = inputField,
    colors = SearchBarDefaults.appBarWithSearchColors(
      scrolledSearchBarContainerColor = Color.Unspecified,
      appBarContainerColor = Color.Unspecified,
      scrolledAppBarContainerColor = Color.Unspecified,
    ),
  )

  if (adaptiveInfo.hasHinges) {
    // targetValue includes the first expansion frame, before the animation has progressed.
    if (searchBarState.targetValue == SearchBarValue.Expanded ||
      searchBarState.currentValue == SearchBarValue.Expanded
    ) {
      HingeSafeDialog(onDismissRequest = closeSearchBar) {
        val focusRequester = remember { FocusRequester() }
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
          Column {
            OutlinedTextField(
              state = textFieldState,
              lineLimits = TextFieldLineLimits.SingleLine,
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
              onKeyboardAction = { closeSearchBar() },
              modifier = Modifier.fillMaxWidth().padding(8.dp).focusRequester(focusRequester),
              placeholder = { Text(stringResource(Res.string.email_hint_search)) },
              leadingIcon = {
                TooltipIconButton(
                  icon = Icons.Rounded.ArrowBackIosNew,
                  label = stringResource(Res.string.email_action_search_collapsed),
                  onClick = closeSearchBar,
                )
              },
            )
            Box(Modifier.weight(1f, fill = false)) {
              outputField(this@Column)
            }
          }
        }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
      }
    }
  } else if (useFullScreenSearchBar) {
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
private fun SearchResults(
  searchResults: LazyPagingItems<EmailModel>,
  scrollState: LazyListState,
  onResultClick: (Long) -> Unit,
) {
  if (searchResults.itemCount <= 0) {
    Text(
      text = stringResource(Res.string.email_msg_no_item_found),
      modifier = Modifier.padding(16.dp),
    )
    return
  }

  LazyColumn(modifier = Modifier.fillMaxWidth(), state = scrollState) {
    items(count = searchResults.itemCount, key = searchResults.itemKey { it.id }) {
      val email = searchResults[it] ?: return@items
      val sender = email.sender
      ListItem(
        headlineContent = { Text(email.subject) },
        supportingContent = { Text(sender.fullName) },
        leadingContent = {
          ProfileImage(
            drawableKey = sender.avatar,
            description = stringResource(Res.string.email_action_owner_info)
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
  scrollState: ScrollState,
  scrollable: Boolean,
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
      .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
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
