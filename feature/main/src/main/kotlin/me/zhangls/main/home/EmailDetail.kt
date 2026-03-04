package me.zhangls.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.StarOutline
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.model.toDomain
import me.zhangls.framework.ext.withDebounce
import me.zhangls.main.R
import me.zhangls.theme.component.CenteredTopAppBar

/**
 * @author zhangls
 */
@Composable
fun EmailDetail(
  isBottomNavigationBar: Boolean,
  emailId: Long,
  viewmodel: HomeViewModel,
  onBackPressed: () -> Unit = {}
) {
  val model by viewmodel.getEmail(emailId).collectAsStateWithLifecycle(null)
  val threads = viewmodel.getThreadEmails(emailId).collectAsLazyPagingItems()

  Scaffold(
    topBar = {
      model?.email?.subject?.let {
        CenteredTopAppBar(title = it, navigate = if (isBottomNavigationBar) onBackPressed else null)
      }
    }
  ) { padding ->
    LazyColumn(contentPadding = padding) {
      item {
        model?.let {
          ThreadItem(model = it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
      }
      items(threads.itemCount) {
        val item = threads[it] ?: return@items
        ThreadItem(model = item, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
      }
    }
  }
}

@Composable
fun ThreadItem(model: EmailConvertModel, modifier: Modifier = Modifier, onFavoriteClick: () -> Unit = {}) {
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
      Row(modifier = Modifier.fillMaxWidth()) {
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
          onClick = { onFavoriteClick() }.withDebounce(),
          modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
          Icon(
            imageVector = Icons.Rounded.StarOutline,
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
  Button(
    onClick = onClick.withDebounce(),
    modifier = modifier,
    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
  ) {
    Text(
      text = stringResource(id = textResId),
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}