package me.zhangls.email.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.EmailModel
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Star
import me.zhangls.theme.icon.StarFill
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_action_cancel_favorite
import notes.feature.email.generated.resources.email_action_favorite
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
  toggleSelection: (Long) -> Unit,
  onFavoriteClick: (Long) -> Unit = {},
) {
  val sender = model.sender

  Card(
    modifier = modifier
      .semantics { selected = isSelected }
      .clip(CardDefaults.shape)
      .combinedClickable(
        onClick = {
          if (isMultiSelect) toggleSelection(model.id) else navigateToDetail(model.id)
        },
        onLongClick = { toggleSelection(model.id) },
      ),
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
        FlipAvatar(isSelected = isSelected, account = sender)

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
            text = model.createdAt,
            style = MaterialTheme.typography.labelMedium,
          )
        }
        IconButton(
          onClick = { onFavoriteClick(model.id) },
          modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
          Icon(
            imageVector = if (model.isImportant) Icons.Rounded.StarFill else Icons.Rounded.Star,
            contentDescription = stringResource(
              if (model.isImportant) Res.string.email_action_cancel_favorite
              else Res.string.email_action_favorite
            ),
            tint = MaterialTheme.colorScheme.outline,
          )
        }
      }

      Text(
        text = model.subject.toDisplaySubject(),
        style = MaterialTheme.typography.bodyLarge,
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

@Composable
private fun FlipAvatar(modifier: Modifier = Modifier, isSelected: Boolean, account: AccountModel) {
  val rotation by animateFloatAsState(
    targetValue = if (isSelected) 180F else 0F,
    animationSpec = tween(durationMillis = 300),
    label = "rotation"
  )

  Box(
    modifier = modifier
      .graphicsLayer {
        rotationY = rotation
        cameraDistance = 12 * density
      }
  ) {
    if (rotation <= 90F) {
      ProfileImage(drawableKey = account.avatar, description = account.fullName)
    }
    if (rotation > 90F) {
      SelectedProfileImage(modifier = Modifier.graphicsLayer {
        // 修正背面的图像
        rotationY = 180F
      })
    }
  }
}
