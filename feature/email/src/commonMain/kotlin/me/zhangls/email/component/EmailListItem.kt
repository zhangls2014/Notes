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
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.toDomain
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Star
import me.zhangls.theme.icon.StarFill

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Loading(modifier: Modifier = Modifier) {
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
