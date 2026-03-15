package me.zhangls.main.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.model.toDomain
import me.zhangls.framework.ext.withDebounce
import me.zhangls.main.EmailIntent
import me.zhangls.main.EmailViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EmailList(
  contentPadding: PaddingValues = PaddingValues(0.dp),
  emailListState: LazyListState,
  selectedItems: Set<Long>,
  viewmodel: EmailViewModel,
  isFavorite: Boolean = false,
  openedEmailId: Long? = null,
  navigateToDetail: (Long) -> Unit,
) {
  val emailItems = viewmodel.emailPaging.collectAsLazyPagingItems()

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
      EmailItem(
        model = item,
        modifier = Modifier
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .animateItem(),
        isMultiSelect = if (isFavorite) false else selectedItems.isNotEmpty(),
        isOpened = item.email.id == openedEmailId,
        isSelected = if (isFavorite) false else selectedItems.contains(item.email.id),
        navigateToDetail = navigateToDetail,
        toggleSelection = { viewmodel.sendIntent(EmailIntent.UpdateSelectedEmail(it)) },
        onFavoriteClick = { viewmodel.sendIntent(EmailIntent.UpdateFavorite(item.email.id)) },
      )
    }

    if (emailItems.loadState.append == LoadState.Loading) {
      item { Loading(modifier = Modifier.fillParentMaxWidth()) }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Loading(modifier: Modifier) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    LoadingIndicator()
  }
}

@Composable
fun EmailItem(
  model: EmailConvertModel,
  modifier: Modifier = Modifier,
  isMultiSelect: Boolean = false,
  isOpened: Boolean = false,
  isSelected: Boolean = false,
  navigateToDetail: (Long) -> Unit,
  toggleSelection: (Long) -> Unit,
  onFavoriteClick: () -> Unit = {},
) {
  val sender = model.sender.toDomain()
  val email = model.email
  val favoriteClick = remember(email.id, onFavoriteClick) {
    { onFavoriteClick() }.withDebounce()
  }

  Card(
    modifier = modifier
      .semantics { selected = isSelected }
      .clip(CardDefaults.shape)
      .combinedClickable(
        onClick = {
          if (isMultiSelect) toggleSelection(email.id) else navigateToDetail(email.id)
        },
        onLongClick = { toggleSelection(email.id) },
      )
      .clip(CardDefaults.shape),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
      else if (isOpened) MaterialTheme.colorScheme.secondaryContainer
      else MaterialTheme.colorScheme.surfaceVariant,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AnimatedContent(targetState = isSelected, label = "avatar") {
          if (it) {
            SelectedProfileImage()
          } else {
            ProfileImage(drawableResource = sender.avatar, description = sender.fullName)
          }
        }

        Column(
          modifier = Modifier
            .weight(1F)
            .padding(horizontal = 12.dp, vertical = 4.dp),
          verticalArrangement = Arrangement.Center,
        ) {
          Text(
            text = sender.firstName,
            style = MaterialTheme.typography.labelMedium,
          )
          Text(
            text = email.createdAt,
            style = MaterialTheme.typography.labelMedium,
          )
        }
        IconButton(
          onClick = favoriteClick,
          modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
          Icon(
            imageVector = if (email.isImportant) Icons.Rounded.Star else Icons.Rounded.StarOutline,
            contentDescription = "Favorite",
            tint = MaterialTheme.colorScheme.outline,
          )
        }
      }

      Text(
        text = email.subject,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
      )
      Text(
        text = email.body,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun ProfileImage(drawableResource: Int, description: String) {
  Image(
    modifier = Modifier
      .size(40.dp)
      .clip(CircleShape),
    painter = painterResource(id = drawableResource),
    contentScale = ContentScale.Crop,
    contentDescription = description,
  )
}

@Composable
fun SelectedProfileImage() {
  Box(
    modifier = Modifier
      .size(40.dp)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.primary),
  ) {
    Icon(
      imageVector = Icons.Rounded.Check,
      contentDescription = null,
      modifier = Modifier
        .size(24.dp)
        .align(Alignment.Center),
      tint = MaterialTheme.colorScheme.onPrimary,
    )
  }
}
