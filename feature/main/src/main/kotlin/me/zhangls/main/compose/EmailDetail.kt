package me.zhangls.main.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.model.toDomain
import me.zhangls.framework.ext.withDebounce
import me.zhangls.main.EmailIntent
import me.zhangls.main.EmailViewModel
import me.zhangls.main.R
import me.zhangls.main.icon.Star
import me.zhangls.main.icon.StarFill
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.icon.Icons

/**
 * @author zhangls
 */
@Composable
internal fun EmailDetail(
  emailId: Long,
  isFavorite: Boolean,
  isBottomNavigationBar: Boolean,
  viewmodel: EmailViewModel,
  onBackPressed: (() -> Unit)?
) {
  val emailFlow = remember(viewmodel, emailId) { viewmodel.getEmail(emailId) }
  val threadFlow = remember(viewmodel, emailId) { viewmodel.getThreadEmails(emailId) }
  val model by emailFlow.collectAsStateWithLifecycle(null)
  val threads = threadFlow.collectAsLazyPagingItems()

  Scaffold(
    topBar = {
      model?.email?.subject?.let {
        CenteredTopAppBar(title = it, navigate = onBackPressed)
      }
    }
  ) { padding ->
    val contentPadding = if (isFavorite) {
      padding
    } else {
      PaddingValues(
        top = padding.calculateTopPadding(),
        bottom = padding.calculateBottomPadding(),
        start = if (isBottomNavigationBar) padding.calculateStartPadding(LayoutDirection.Ltr) else 0.dp,
        end = padding.calculateEndPadding(LayoutDirection.Ltr)
      )
    }

    LazyColumn(contentPadding = contentPadding) {
      item {
        model?.let {
          EmailDetailItem(model = it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { id ->
            viewmodel.sendIntent(EmailIntent.UpdateFavorite(id))
          }
        }
      }
      items(count = threads.itemCount, key = threads.itemKey { it.email.id }) {
        val item = threads[it] ?: return@items
        EmailDetailItem(model = item, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { id ->
          viewmodel.sendIntent(EmailIntent.UpdateFavorite(id))
        }
      }
    }
  }
}

@Composable
fun EmailDetailItem(model: EmailConvertModel, modifier: Modifier = Modifier, onFavoriteClick: (Long) -> Unit = {}) {
  val sender = model.sender.toDomain()
  val email = model.email
  val favoriteClick = remember(email.id, onFavoriteClick) {
    { onFavoriteClick(email.id) }.withDebounce()
  }

  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ProfileImage(
          drawableResource = sender.avatar,
          description = sender.fullName,
        )

        Column(
          modifier = Modifier
            .weight(1f)
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
            color = MaterialTheme.colorScheme.outline,
          )
        }
        IconButton(
          onClick = favoriteClick,
          modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
          Icon(
            imageVector = if (email.isImportant) Icons.Rounded.StarFill else Icons.Rounded.Star,
            contentDescription = "Favorite",
            tint = MaterialTheme.colorScheme.outline,
          )
        }
      }

      Text(
        text = email.subject,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
      )

      Text(
        text = email.body,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        ReplayButton(modifier = Modifier.weight(1F), textResId = R.string.main_action_email_reply) { }
        ReplayButton(modifier = Modifier.weight(1F), textResId = R.string.main_action_email_reply_all) { }
      }
    }
  }
}

@Preview
@Composable
private fun ReplayButton(
  modifier: Modifier = Modifier,
  textResId: Int = R.string.main_action_email_reply,
  onClick: () -> Unit = {}
) {
  val replayClick = remember(textResId, onClick) { onClick.withDebounce() }
  Button(
    onClick = replayClick,
    modifier = modifier,
    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
  ) {
    Text(
      text = stringResource(id = textResId),
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
