package me.zhangls.email.component

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.zhangls.data.model.EmailModel
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_action_open_email
import notes.feature.email.generated.resources.email_action_select_email
import notes.feature.email.generated.resources.email_action_deselect_email
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun Loading(modifier: Modifier = Modifier) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    LoadingIndicator()
  }
}

@Composable
fun EmailListItem(
  model: EmailModel,
  modifier: Modifier = Modifier,
  isMultiSelect: Boolean = false,
  isOpened: Boolean = false,
  isSelected: Boolean = false,
  navigateToDetail: (Long) -> Unit,
  // null 表示此列表不支持选择，也不暴露长按动作。
  toggleSelection: ((Long) -> Unit)?,
  onFavoriteClick: (Long) -> Unit = {},
) {
  val selectionLabel = stringResource(
    if (isSelected) Res.string.email_action_deselect_email else Res.string.email_action_select_email,
  )
  val openLabel = stringResource(Res.string.email_action_open_email)
  Card(
    modifier = modifier
      .semantics { selected = isSelected }
      .clip(MaterialTheme.shapes.large)
      .combinedClickable(
        onClickLabel = if (isMultiSelect && toggleSelection != null) selectionLabel else openLabel,
        onClick = {
          if (isMultiSelect && toggleSelection != null) toggleSelection(model.id) else navigateToDetail(model.id)
        },
        onLongClickLabel = if (toggleSelection != null) selectionLabel else null,
        onLongClick = toggleSelection?.let { toggle -> { toggle(model.id) } },
      ),
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
      else if (isOpened) MaterialTheme.colorScheme.secondaryContainer
      else MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
    ) {
      EmailHeader(
        account = model.sender,
        createdAt = model.createdAt,
        isImportant = model.isImportant,
        variant = EmailHeaderVariant.ListItem,
        isSelected = isSelected,
        onFavoriteClick = { onFavoriteClick(model.id) },
      )

      Text(
        text = model.subject.toDisplaySubject(),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
      )
      Text(
        text = model.body,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
