package me.zhangls.email.component

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import me.zhangls.email.mvi.EmailIntent
import me.zhangls.email.mvi.EmailViewModel

/**
 * 邮件列表页骨架：组装顶栏（搜索/多选操作栏）、写邮件 FAB、分页列表与草稿 BottomSheet。
 * 各子职责分别位于 [EmailTopBar]、[EmailFab]、[EmailPagedList]、[NewEmailSheet]。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun EmailList(
  viewModel: EmailViewModel,
  isFavorite: Boolean,
  openedEmailId: Long? = null,
  navigateToDetail: (Long) -> Unit,
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val emailListState = rememberLazyListState()
  val emailItems = if (isFavorite) {
    viewModel.emailFavoritePaging.collectAsLazyPagingItems()
  } else {
    viewModel.emailPaging.collectAsLazyPagingItems()
  }
  val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

  Scaffold(
    modifier = if (isFavorite) Modifier else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      if (isFavorite) return@Scaffold
      EmailTopBar(
        state = state,
        scrollBehavior = scrollBehavior,
        onIntent = { viewModel.sendIntent(it) },
        onResultClick = navigateToDetail
      )
    },
    floatingActionButton = {
      if (isFavorite) return@Scaffold
      NewEmailFab(
        isSending = state.isSending,
        listState = emailListState,
        onOpenDraft = { viewModel.sendIntent(EmailIntent.OpenDraft) },
      )
    },
  ) { padding ->
    EmailPagedList(
      emailItems = emailItems,
      listState = emailListState,
      // 直接用 Scaffold 给的 padding：导航套件占用的那部分系统内边距已由外壳消费掉
      // （`AppShell`），这里读到的是"内容区真正可用"的内边距，不需要再换算。
      contentPadding = padding,
      isFavorite = isFavorite,
      selectedItems = state.selectedItems,
      openedEmailId = openedEmailId,
      navigateToDetail = navigateToDetail,
      onIntent = { viewModel.sendIntent(it) },
    )
  }

  if (!isFavorite) {
    NewEmailSheet(
      recipients = state.accounts,
      visible = state.isEmailDraftVisible,
      selectedRecipientIds = state.draftRecipientIds,
      subject = state.draftSubject,
      body = state.draftBody,
      isSending = state.isSending,
      onToggleRecipient = { id, selected ->
        viewModel.sendIntent(EmailIntent.UpdateDraftRecipient(recipientId = id, selected = selected))
      },
      onSubjectChange = { viewModel.sendIntent(EmailIntent.UpdateDraftSubject(it)) },
      onBodyChange = { viewModel.sendIntent(EmailIntent.UpdateDraftBody(it)) },
      onDismiss = { viewModel.sendIntent(EmailIntent.CloseDraft) },
      onCancel = { viewModel.sendIntent(EmailIntent.ClearDraft) },
      onSave = { viewModel.sendIntent(EmailIntent.SubmitDraft) },
    )
  }
}
