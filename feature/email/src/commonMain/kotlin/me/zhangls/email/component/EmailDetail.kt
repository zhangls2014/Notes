package me.zhangls.email.component

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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.model.toDomain
import me.zhangls.theme.icon.Star
import me.zhangls.theme.icon.StarFill
import me.zhangls.email.waterfall.EmailIntent
import me.zhangls.email.waterfall.EmailViewModel
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.icon.Icons
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_action_email_reply
import notes.feature.email.generated.resources.email_action_email_reply_all
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * @author zhangls
 */
@Composable
internal fun EmailDetail(
  emailId: Long,
  isStandalone: Boolean,
  isBottomNavigationBar: Boolean,
  viewModel: EmailViewModel,
  onBackPressed: (() -> Unit)?
) {
  val emailFlow = remember(viewModel, emailId) { viewModel.getEmail(emailId) }
  val threadFlow = remember(viewModel, emailId) { viewModel.getThreadEmails(emailId) }
  val model by emailFlow.collectAsStateWithLifecycle(null)
  val threads = threadFlow.collectAsLazyPagingItems()

  Scaffold(
    topBar = {
      model?.email?.subject?.let {
        CenteredTopAppBar(title = it.toDisplaySubject(), navigate = onBackPressed)
      }
    }
  ) { padding ->
    val contentPadding = if (isStandalone) {
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
            viewModel.sendIntent(EmailIntent.UpdateFavorite(id))
          }
        }
      }
      items(count = threads.itemCount, key = threads.itemKey { it.email.id }) {
        val item = threads[it] ?: return@items
        EmailDetailItem(model = item, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { id ->
          viewModel.sendIntent(EmailIntent.UpdateFavorite(id))
        }
      }
    }
  }
}

@Composable
fun EmailDetailItem(model: EmailConvertModel, modifier: Modifier = Modifier, onFavoriteClick: (Long) -> Unit = {}) {
  val sender = model.sender.toDomain()
  val email = model.email

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
        ProfileImage(drawableKey = sender.avatar, description = sender.fullName)

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
          onClick = { onFavoriteClick(email.id) },
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
        text = email.subject.toDisplaySubject(),
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
        // TODO 回复/回复全部功能尚未实现
        ReplyButton(modifier = Modifier.weight(1F), textRes = Res.string.email_action_email_reply) { }
        ReplyButton(modifier = Modifier.weight(1F), textRes = Res.string.email_action_email_reply_all) { }
      }
    }
  }
}

@Composable
private fun ReplyButton(
  modifier: Modifier = Modifier,
  textRes: StringResource = Res.string.email_action_email_reply,
  onClick: () -> Unit = {}
) {
  Button(
    onClick = onClick,
    modifier = modifier,
    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
  ) {
    Text(
      text = stringResource(textRes),
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
