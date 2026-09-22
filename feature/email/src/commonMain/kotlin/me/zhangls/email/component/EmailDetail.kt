package me.zhangls.email.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import me.zhangls.data.model.EmailModel
import me.zhangls.email.mvi.EmailIntent
import me.zhangls.email.mvi.EmailViewModel
import me.zhangls.theme.component.AdaptiveContent
import me.zhangls.theme.component.CenteredTopAppBar
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
  viewModel: EmailViewModel,
  onBackPressed: (() -> Unit)?
) {
  val emailFlow = remember(viewModel, emailId) { viewModel.getEmail(emailId) }
  val threadFlow = remember(viewModel, emailId) { viewModel.getThreadEmails(emailId) }
  val model by emailFlow.collectAsStateWithLifecycle(null)
  val threads = threadFlow.collectAsLazyPagingItems()

  Scaffold(
    topBar = {
      model?.subject?.let {
        CenteredTopAppBar(title = it.toDisplaySubject(), navigate = onBackPressed)
      }
    }
  ) { padding ->
    // 直接用 Scaffold 给的 padding：导航套件占用的那部分系统内边距已由外壳消费掉，
    // 读到的是"内容区真正可用"的内边距。本组件无论作为首页分栏的详情、还是作为独立的
    // 导航目的地，都在同一个外壳内，因此也不需要"我是否独立"这个参数。
    //
    // 正文再限一个可读宽度：Expanded 窗口（840dp 起）上详情栏可以宽到 800dp 以上，
    // 一行正文过长会显著降低可读性。这是可读性常量，与窗口形态无关，
    // 因此不属于"多形态适配"（那部分由 LocalWindowAdaptiveInfo 与场景策略负责）。
    AdaptiveContent(maxWidth = DetailMaxWidth) {
      LazyColumn(contentPadding = padding) {
        item {
          model?.let {
            EmailDetailItem(model = it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { id ->
              viewModel.sendIntent(EmailIntent.UpdateFavorite(id))
            }
          }
        }
        items(count = threads.itemCount, key = threads.itemKey { it.id }) {
          val item = threads[it] ?: return@items
          EmailDetailItem(model = item, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { id ->
            viewModel.sendIntent(EmailIntent.UpdateFavorite(id))
          }
        }
      }
    }
  }
}

/** 邮件正文的可读宽度上限。 */
private val DetailMaxWidth = 720.dp

@Composable
fun EmailDetailItem(model: EmailModel, modifier: Modifier = Modifier, onFavoriteClick: (Long) -> Unit = {}) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
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
        variant = EmailHeaderVariant.Detail,
        onFavoriteClick = { onFavoriteClick(model.id) },
      )

      Text(
        text = model.subject.toDisplaySubject(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
      )

      Text(
        text = model.body,
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
