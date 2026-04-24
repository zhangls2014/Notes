package me.zhangls.email.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.launch
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.toDomain
import me.zhangls.email.icon.Cancel
import me.zhangls.email.icon.Delete
import me.zhangls.email.icon.Edit
import me.zhangls.email.icon.Star
import me.zhangls.email.icon.StarFill
import me.zhangls.email.search.EmailSearchBar
import me.zhangls.email.waterfall.EmailIntent
import me.zhangls.email.waterfall.EmailViewModel
import me.zhangls.theme.icon.Icons
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.allDrawableResources
import notes.feature.email.generated.resources.email_action_cancel
import notes.feature.email.generated.resources.email_action_cancel_favorite
import notes.feature.email.generated.resources.email_action_delete
import notes.feature.email.generated.resources.email_action_favorite
import notes.feature.email.generated.resources.email_action_new_email
import notes.feature.email.generated.resources.email_action_save
import notes.feature.email.generated.resources.email_action_sending
import notes.feature.email.generated.resources.email_label_body
import notes.feature.email.generated.resources.email_label_recipients
import notes.feature.email.generated.resources.email_label_subject
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun EmailList(
  viewmodel: EmailViewModel,
  isFavorite: Boolean,
  isBottomNavigationBar: Boolean,
  openedEmailId: Long? = null,
  navigateToDetail: (Long) -> Unit,
) {
  val state by viewmodel.state.collectAsStateWithLifecycle()
  val emailListState = rememberLazyListState()
  val emailItems = if (isFavorite) {
    viewmodel.emailFavoritePaging.collectAsLazyPagingItems()
  } else {
    viewmodel.emailPaging.collectAsLazyPagingItems()
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
                viewmodel.sendIntent(EmailIntent.MultiFavorite)
              }

              Res.string.email_action_cancel_favorite -> {
                viewmodel.sendIntent(EmailIntent.MultiCancelFavorite)
              }

              Res.string.email_action_delete -> {
                viewmodel.sendIntent(EmailIntent.MultiDelete)
              }

              Res.string.email_action_cancel -> {
                viewmodel.sendIntent(EmailIntent.ClearSelectedEmail)
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
        onClick = { if (!state.isSending) viewmodel.sendIntent(EmailIntent.OpenDraft) },
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
            viewmodel.sendIntent(EmailIntent.UpdateSelectedEmail(it))
          },
          onFavoriteClick = {
            viewmodel.sendIntent(EmailIntent.UpdateFavorite(it))
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
        viewmodel.sendIntent(EmailIntent.UpdateDraftRecipient(recipientId = id, selected = selected))
      },
      onSubjectChange = { viewmodel.sendIntent(EmailIntent.UpdateDraftSubject(it)) },
      onBodyChange = { viewmodel.sendIntent(EmailIntent.UpdateDraftBody(it)) },
      onDismiss = { viewmodel.sendIntent(EmailIntent.CloseDraft) },
      onCancel = { viewmodel.sendIntent(EmailIntent.ClearDraft) },
      onSave = { viewmodel.sendIntent(EmailIntent.SubmitDraft) },
    )
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
fun EmailListItem(
  model: EmailConvertModel,
  modifier: Modifier = Modifier,
  isMultiSelect: Boolean = false,
  isOpened: Boolean = false,
  isSelected: Boolean = false,
  navigateToDetail: (Long) -> Unit,
  toggleSelection: (Long) -> Unit,
  onFavoriteClick: (Long) -> Unit = {},
) {
  val sender = model.sender.toDomain()
  val email = model.email

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
            ProfileImage(drawableKey = sender.avatar, description = sender.fullName)
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
          onClick = { onFavoriteClick(email.id) },
          modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
          Icon(
            imageVector = if (email.isImportant) Icons.Rounded.StarFill else Icons.Rounded.Star,
            contentDescription = "Favorite",
            tint = MaterialTheme.colorScheme.outline,
          )
        }
      }

      Text(
        text = email.subject.toDisplaySubject(),
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun NewEmailSheet(
  recipients: List<AccountModel>,
  visible: Boolean,
  selectedRecipientIds: Set<Long>,
  subject: String,
  body: String,
  isSending: Boolean,
  onToggleRecipient: (Long, Boolean) -> Unit,
  onSubjectChange: (String) -> Unit,
  onBodyChange: (String) -> Unit,
  onDismiss: () -> Unit,
  onCancel: () -> Unit,
  onSave: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()


  if (visible) {
    scope.launch { sheetState.show() }
  } else if (sheetState.isVisible) {
    scope.launch { sheetState.hide() }
  } else if (sheetState.isAnimationRunning.not()) {
    return
  }


  ModalBottomSheet(onDismissRequest = { if (!isSending) onDismiss() }, sheetState = sheetState) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .verticalScroll(state = rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(Res.string.email_label_recipients),
        style = MaterialTheme.typography.titleMedium,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        recipients.forEach { recipient ->
          FilterChip(
            selected = selectedRecipientIds.contains(recipient.id),
            onClick = { onToggleRecipient(recipient.id, !selectedRecipientIds.contains(recipient.id)) },
            enabled = !isSending,
            leadingIcon = {
              Image(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape),
                painter = painterResource(Res.allDrawableResources.getValue(recipient.avatar)),
                contentScale = ContentScale.Crop,
                contentDescription = null,
              )
            },
            label = { Text(recipient.fullName) },
          )
        }
      }

      OutlinedTextField(
        value = subject,
        onValueChange = onSubjectChange,
        enabled = !isSending,
        label = { Text(text = stringResource(Res.string.email_label_subject)) },
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = body,
        onValueChange = onBodyChange,
        enabled = !isSending,
        label = { Text(text = stringResource(Res.string.email_label_body)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 4,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(enabled = !isSending, onClick = onCancel) {
          Text(text = stringResource(Res.string.email_action_cancel))
        }
        Spacer(modifier = Modifier.size(8.dp))
        Button(enabled = !isSending, onClick = onSave) {
          Text(text = stringResource(Res.string.email_action_save))
        }
      }
    }
  }
}
