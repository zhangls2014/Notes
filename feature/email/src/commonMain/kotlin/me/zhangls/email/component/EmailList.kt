package me.zhangls.email.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import me.zhangls.email.search.EmailSearchBar
import me.zhangls.email.waterfall.EmailIntent
import me.zhangls.email.waterfall.EmailViewModel
import me.zhangls.theme.icon.Cancel
import me.zhangls.theme.icon.Delete
import me.zhangls.theme.icon.Edit
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Star
import me.zhangls.theme.icon.StarFill
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_action_cancel
import notes.feature.email.generated.resources.email_action_cancel_favorite
import notes.feature.email.generated.resources.email_action_delete
import notes.feature.email.generated.resources.email_action_favorite
import notes.feature.email.generated.resources.email_action_new_email
import notes.feature.email.generated.resources.email_action_sending
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun EmailList(
  viewModel: EmailViewModel,
  isFavorite: Boolean,
  isBottomNavigationBar: Boolean,
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
  val items = remember {
    listOf(
      ActionItem(Icons.Rounded.StarFill, Res.string.email_action_favorite),
      ActionItem(Icons.Rounded.Star, Res.string.email_action_cancel_favorite),
      ActionItem(Icons.Rounded.Delete, Res.string.email_action_delete),
      ActionItem(Icons.Rounded.Cancel, Res.string.email_action_cancel),
    )
  }

  Scaffold(
    modifier = if (isFavorite) Modifier else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      if (isFavorite) return@Scaffold
      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        if (state.selectedItems.isEmpty()) {
          EmailSearchBar(
            isBottomNavigationBar = isBottomNavigationBar,
            scrollBehavior = scrollBehavior,
            onResultClick = navigateToDetail
          )
        } else {
          EmailActionBar(modifier = Modifier.statusBarsPadding(), items = items) {
            when (it.text) {
              Res.string.email_action_favorite -> {
                viewModel.sendIntent(EmailIntent.MultiFavorite)
              }

              Res.string.email_action_cancel_favorite -> {
                viewModel.sendIntent(EmailIntent.MultiCancelFavorite)
              }

              Res.string.email_action_delete -> {
                viewModel.sendIntent(EmailIntent.MultiDelete)
              }

              Res.string.email_action_cancel -> {
                viewModel.sendIntent(EmailIntent.ClearSelectedEmail)
              }
            }
          }
        }
      }
    },
    floatingActionButton = {
      if (isFavorite) return@Scaffold
      ExtendedFloatingActionButton(
        text = {
          Text(
            text = if (state.isSending) {
              stringResource(Res.string.email_action_sending)
            } else {
              stringResource(Res.string.email_action_new_email)
            }
          )
        },
        icon = {
          if (state.isSending) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = stringResource(Res.string.email_action_new_email)
            )
          }
        },
        onClick = { if (!state.isSending) viewModel.sendIntent(EmailIntent.OpenDraft) },
        expanded = emailListState.lastScrolledBackward || emailListState.canScrollBackward.not(),
      )
    },
  ) { padding ->
    val contentPadding = PaddingValues(
      top = padding.calculateTopPadding(),
      bottom = if (isBottomNavigationBar) 0.dp else padding.calculateBottomPadding(),
      start = if (isBottomNavigationBar) padding.calculateStartPadding(LayoutDirection.Ltr) else 0.dp,
      end = padding.calculateEndPadding(LayoutDirection.Ltr)
    )

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = contentPadding, state = emailListState) {
      if (emailItems.loadState.refresh == LoadState.Loading) {
        item { Loading(modifier = Modifier.fillParentMaxSize()) }
      }
      if (emailItems.loadState.refresh is LoadState.NotLoading && emailItems.itemCount == 0) {
        item {
          Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No emails!")
          }
        }
      }
      if (emailItems.loadState.prepend == LoadState.Loading) {
        item { Loading(modifier = Modifier.fillParentMaxWidth()) }
      }

      items(count = emailItems.itemCount, key = emailItems.itemKey { it.email.id }) { index ->
        val item = emailItems[index] ?: return@items
        EmailListItem(
          model = item,
          modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateItem(),
          isMultiSelect = if (isFavorite) false else state.selectedItems.isNotEmpty(),
          isOpened = item.email.id == openedEmailId,
          isSelected = if (isFavorite) false else state.selectedItems.contains(item.email.id),
          navigateToDetail = navigateToDetail,
          toggleSelection = {
            if (isFavorite) return@EmailListItem
            viewModel.sendIntent(EmailIntent.UpdateSelectedEmail(it))
          },
          onFavoriteClick = {
            viewModel.sendIntent(EmailIntent.UpdateFavorite(it))
          },
        )
      }

      if (emailItems.loadState.append == LoadState.Loading) {
        item { Loading(modifier = Modifier.fillParentMaxWidth()) }
      }
    }
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
