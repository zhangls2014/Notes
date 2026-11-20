package me.zhangls.email.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.collections.immutable.ImmutableSet
import me.zhangls.data.model.EmailModel
import me.zhangls.email.mvi.EmailIntent
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_all_shown
import notes.feature.email.generated.resources.email_append_failed
import notes.feature.email.generated.resources.email_load_failed
import notes.feature.email.generated.resources.email_load_failed_description
import notes.feature.email.generated.resources.email_msg_no_emails
import notes.feature.email.generated.resources.email_refresh_failed
import org.jetbrains.compose.resources.stringResource

/**
 * 邮件分页列表主体：加载态、空态与列表项渲染。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EmailPagedList(
  emailItems: LazyPagingItems<EmailModel>,
  listState: LazyListState,
  contentPadding: PaddingValues,
  isFavorite: Boolean,
  selectedItems: ImmutableSet<Long>,
  openedEmailId: Long?,
  navigateToDetail: (Long) -> Unit,
  onIntent: (EmailIntent) -> Unit,
  bottomActionHeight: Dp = 0.dp,
) {
  val refreshState = rememberPullToRefreshState()
  val isRefreshing = emailItems.loadState.refresh is LoadState.Loading
  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = { emailItems.refresh() },
    modifier = Modifier.fillMaxSize(),
    state = refreshState,
    indicator = {
      PullToRefreshDefaults.Indicator(
        state = refreshState,
        isRefreshing = isRefreshing,
        modifier = Modifier.align(Alignment.TopCenter).padding(top = contentPadding.calculateTopPadding()),
      )
    },
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = contentPadding,
      state = listState,
    ) {
      emailListHeader(emailItems)
      items(count = emailItems.itemCount, key = emailItems.itemKey { it.id }) { index ->
        val item = emailItems[index] ?: return@items
        EmailListItem(
          model = item,
          modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateItem(),
          isMultiSelect = if (isFavorite) false else selectedItems.isNotEmpty(),
          isOpened = item.id == openedEmailId,
          isSelected = if (isFavorite) false else selectedItems.contains(item.id),
          navigateToDetail = navigateToDetail,
          toggleSelection = if (isFavorite) {
            null
          } else {
            { id ->
              onIntent(EmailIntent.UpdateSelectedEmail(id))
            }
          },
          onFavoriteClick = {
            onIntent(EmailIntent.UpdateFavorite(it))
          },
        )
      }

      emailListFooter(emailItems, bottomActionHeight)
    }
    EmailListPlaceholder(emailItems, contentPadding)
  }
}

private fun LazyListScope.emailListHeader(emailItems: LazyPagingItems<EmailModel>) {
  if (emailItems.loadState.refresh is LoadState.NotLoading && emailItems.itemCount == 0) {
    item {
      Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = stringResource(Res.string.email_msg_no_emails),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.outline,
        )
      }
    }
  }
  if (emailItems.loadState.prepend == LoadState.Loading) {
    item { Loading(modifier = Modifier.fillParentMaxWidth()) }
  }

  if (emailItems.loadState.refresh is LoadState.Error && emailItems.itemCount > 0) {
    item {
      EmailInlineError(stringResource(Res.string.email_refresh_failed)) { emailItems.retry() }
    }
  }
}

private fun LazyListScope.emailListFooter(emailItems: LazyPagingItems<EmailModel>, bottomActionHeight: Dp) {
  if (emailItems.itemCount == 0) return
  // The FAB is 16dp above the content edge; keep another 16dp between it and the footer.
  val actionClearance = if (bottomActionHeight > 0.dp) bottomActionHeight + 32.dp else 0.dp
  val appendState = emailItems.loadState.append
  if (appendState is LoadState.Error) {
    item {
      Box(Modifier.fillParentMaxWidth().padding(bottom = actionClearance)) {
        EmailInlineError(stringResource(Res.string.email_append_failed)) { emailItems.retry() }
      }
    }
  } else if (appendState is LoadState.Loading) {
    item {
      Box(Modifier.fillParentMaxWidth().padding(bottom = actionClearance)) {
        Loading(modifier = Modifier.fillMaxWidth())
      }
    }
  } else if (emailItems.loadState.refresh is LoadState.NotLoading &&
    appendState is LoadState.NotLoading && appendState.endOfPaginationReached
  ) {
    item {
      Box(
        modifier = Modifier.fillParentMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val dotModifier = Modifier.size(3.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
          Box(dotModifier)
          Text(
            text = stringResource(Res.string.email_all_shown),
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
          Box(dotModifier)
        }
      }
    }
  }
}

@Composable
private fun EmailListPlaceholder(emailItems: LazyPagingItems<EmailModel>, contentPadding: PaddingValues) {
  // 首次加载不能作为唯一列表项，否则会把恢复中的滚动索引压回 0。
  // 空列表会保留待恢复位置，等真实分页数据到达后再进行布局。
  if (emailItems.loadState.refresh == LoadState.Loading && emailItems.itemCount == 0) {
    Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
      Loading()
    }
  } else if (emailItems.loadState.refresh is LoadState.Error && emailItems.itemCount == 0) {
    Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
      EmailLoadStatus(
        title = stringResource(Res.string.email_load_failed),
        message = stringResource(Res.string.email_load_failed_description),
        modifier = Modifier.verticalScroll(rememberScrollState()),
        onRetry = { emailItems.retry() },
      )
    }
  }
}
