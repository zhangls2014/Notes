package me.zhangls.email.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import me.zhangls.email.mvi.EmailIntent
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_msg_no_emails
import org.jetbrains.compose.resources.stringResource

/**
 * 邮件分页列表主体：加载态、空态与列表项渲染。
 */
@Composable
internal fun EmailPagedList(
  emailItems: LazyPagingItems<me.zhangls.data.database.entity.EmailConvertModel>,
  listState: LazyListState,
  contentPadding: PaddingValues,
  isFavorite: Boolean,
  selectedItems: Set<Long>,
  openedEmailId: Long?,
  navigateToDetail: (Long) -> Unit,
  onIntent: (EmailIntent) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = contentPadding,
    state = listState
  ) {
    if (emailItems.loadState.refresh == LoadState.Loading) {
      item { Loading(modifier = Modifier.fillParentMaxSize()) }
    }
    if (emailItems.loadState.refresh is LoadState.NotLoading && emailItems.itemCount == 0) {
      item {
        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
          Text(text = stringResource(Res.string.email_msg_no_emails))
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
        isMultiSelect = if (isFavorite) false else selectedItems.isNotEmpty(),
        isOpened = item.email.id == openedEmailId,
        isSelected = if (isFavorite) false else selectedItems.contains(item.email.id),
        navigateToDetail = navigateToDetail,
        toggleSelection = {
          if (isFavorite) return@EmailListItem
          onIntent(EmailIntent.UpdateSelectedEmail(it))
        },
        onFavoriteClick = {
          onIntent(EmailIntent.UpdateFavorite(it))
        },
      )
    }

    if (emailItems.loadState.append == LoadState.Loading) {
      item { Loading(modifier = Modifier.fillParentMaxWidth()) }
    }
  }
}
