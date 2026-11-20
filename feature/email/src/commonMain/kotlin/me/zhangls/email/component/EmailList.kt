package me.zhangls.email.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.collections.immutable.toImmutableSet
import me.zhangls.email.mvi.EmailIntent
import me.zhangls.email.mvi.EmailViewModel
import me.zhangls.theme.component.CenteredTopAppBar
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_label_favorites
import org.jetbrains.compose.resources.stringResource

/**
 * 邮件列表页骨架：组装顶栏（收藏标题或搜索/多选操作栏）、写邮件 FAB、分页列表与草稿 BottomSheet。
 * 各子职责分别位于 [EmailTopBar]、[EmailFab]、[EmailPagedList]、[NewEmailSheet]。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun EmailList(
  viewModel: EmailViewModel,
  isFavorite: Boolean,
  navigateToDetail: (Long) -> Unit,
  openedEmailId: Long? = null,
  onProfileClick: () -> Unit = {},
  onDraftVisibilityChange: (Boolean) -> Unit = {},
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  if (!isFavorite) {
    SideEffect { onDraftVisibilityChange(state.isEmailDraftVisible) }
  }
  val emailListState = rememberLazyListState()
  val emailItems = if (isFavorite) {
    viewModel.emailFavoritePaging.collectAsLazyPagingItems()
  } else {
    viewModel.emailPaging.collectAsLazyPagingItems()
  }
  var fabHeight by remember { mutableStateOf(0.dp) }
  val density = LocalDensity.current
  val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

  Scaffold(
    modifier = if (isFavorite) Modifier else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      if (isFavorite) {
        CenteredTopAppBar(title = stringResource(Res.string.email_label_favorites))
      } else {
        EmailTopBar(
          state = state,
          scrollBehavior = scrollBehavior,
          onIntent = { viewModel.sendIntent(it) },
          onResultClick = navigateToDetail,
          onProfileClick = onProfileClick,
        )
      }
    },
    floatingActionButton = {
      if (isFavorite) return@Scaffold
      Box(Modifier.onSizeChanged { fabHeight = with(density) { it.height.toDp() } }) {
        NewEmailFab(
          isSending = state.isSending,
          listState = emailListState,
          onOpenDraft = { viewModel.sendIntent(EmailIntent.OpenDraft) },
        )
      }
    },
  ) { padding ->
    EmailPagedList(
      emailItems = emailItems,
      listState = emailListState,
      // 直接用 Scaffold 给的 padding：导航套件占用的那部分系统内边距已由外壳消费掉
      // （`AppShell`），这里读到的是"内容区真正可用"的内边距，不需要再换算。
      contentPadding = padding,
      bottomActionHeight = if (isFavorite) 0.dp else fabHeight,
      isFavorite = isFavorite,
      selectedItems = state.selectedItems.toImmutableSet(),
      openedEmailId = openedEmailId,
      navigateToDetail = navigateToDetail,
      onIntent = { viewModel.sendIntent(it) },
    )
  }

  if (!isFavorite) {
    EmailDraftEditor(state, viewModel.draftSubjectInput, viewModel.draftBodyInput) { viewModel.sendIntent(it) }
  }
}
