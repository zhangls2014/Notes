package me.zhangls.email.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import me.zhangls.data.model.EmailModel
import me.zhangls.email.mvi.EmailDetailState
import me.zhangls.theme.component.AdaptiveContent
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.icon.Close
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.layout.ContentWidth
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_action_close_detail
import notes.feature.email.generated.resources.email_action_email_reply
import notes.feature.email.generated.resources.email_action_email_reply_all
import notes.feature.email.generated.resources.email_action_forward
import notes.feature.email.generated.resources.email_detail_title
import notes.feature.email.generated.resources.email_load_failed
import notes.feature.email.generated.resources.email_load_failed_description
import notes.feature.email.generated.resources.email_not_found
import notes.feature.email.generated.resources.email_not_found_description
import notes.feature.email.generated.resources.email_thread_failed
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * @author zhangls
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EmailDetail(
  detailState: EmailDetailState,
  threads: LazyPagingItems<EmailModel>,
  onBack: (() -> Unit)?,
  onFavoriteClick: (Long) -> Unit,
  onClose: (() -> Unit)? = null,
  onCompose: (EmailModel, String) -> Unit = { _, _ -> },
  onRetry: () -> Unit = {},
) {
  val model = (detailState as? EmailDetailState.Loaded)?.email
  // Draw the page through the status-bar safe area. Insets belong to its inner content,
  // so the background and safe area stay inside the navigation transform together.
  Scaffold(
    modifier = Modifier.fillMaxSize().testTag("email-detail-page"),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      CenteredTopAppBar(
        title = model?.subject?.toDisplaySubject() ?: stringResource(Res.string.email_detail_title),
        navigate = onBack,
        actions = {
          onClose?.let { close ->
            IconButton(onClick = close, shapes = IconButtonDefaults.shapes()) {
              Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.email_action_close_detail))
            }
          }
        },
      )
    },
  ) { padding ->
    // 直接用 Scaffold 给的 padding：导航套件占用的那部分系统内边距已由外壳消费掉，
    // 读到的是"内容区真正可用"的内边距。本组件无论作为首页分栏的详情、还是作为独立的
    // 导航目的地，都在同一个外壳内，因此也不需要"我是否独立"这个参数。
    //
    // 正文再限一个可读宽度：Expanded 窗口（840dp 起）上详情栏可以宽到 800dp 以上，
    // 一行正文过长会显著降低可读性。这是可读性常量，与窗口形态无关，
    // 因此不属于"多形态适配"（那部分由 LocalWindowAdaptiveInfo 与场景策略负责）。
    if (detailState !is EmailDetailState.Loaded) {
      Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        when (detailState) {
          EmailDetailState.Loading -> Loading()

          EmailDetailState.Failed -> EmailLoadStatus(
            title = stringResource(Res.string.email_load_failed),
            message = stringResource(Res.string.email_load_failed_description),
            modifier = Modifier.verticalScroll(rememberScrollState()),
            onRetry = onRetry,
          )

          EmailDetailState.NotFound -> EmailLoadStatus(
            title = stringResource(Res.string.email_not_found),
            message = stringResource(Res.string.email_not_found_description),
            modifier = Modifier.verticalScroll(rememberScrollState()),
          )
        }
      }
    } else {
      EmailConversation(detailState.email, threads, padding, onFavoriteClick, onCompose)
    }
  }
}

@Composable
private fun EmailConversation(
  model: EmailModel,
  threads: LazyPagingItems<EmailModel>,
  padding: PaddingValues,
  onFavoriteClick: (Long) -> Unit,
  onCompose: (EmailModel, String) -> Unit,
) {
  AdaptiveContent(maxWidth = ContentWidth.Article) {
    LazyColumn(contentPadding = padding) {
      item {
        EmailDetailItem(
          model = model,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          onCompose = onCompose,
        ) { id ->
          onFavoriteClick(id)
        }
      }
      items(count = threads.itemCount, key = threads.itemKey { it.id }) {
        val item = threads[it] ?: return@items
        EmailDetailItem(
          model = item,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          onCompose = onCompose,
        ) { id ->
          onFavoriteClick(id)
        }
      }
      if (threads.loadState.refresh is LoadState.Error || threads.loadState.append is LoadState.Error) {
        item {
          EmailInlineError(stringResource(Res.string.email_thread_failed)) { threads.retry() }
        }
      }
    }
  }
}

@Composable
fun EmailDetailItem(
  model: EmailModel,
  modifier: Modifier = Modifier,
  onCompose: (EmailModel, String) -> Unit = { _, _ -> },
  onFavoriteClick: (Long) -> Unit = {},
) {
  Card(
    modifier = modifier,
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
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
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
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
        ReplyButton(modifier = Modifier.weight(1F), textRes = Res.string.email_action_email_reply) {
          onCompose(model, "reply")
        }
        ReplyButton(modifier = Modifier.weight(1F), textRes = Res.string.email_action_email_reply_all) {
          onCompose(model, "replyAll")
        }
      }
      ReplyButton(textRes = Res.string.email_action_forward) { onCompose(model, "forward") }
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReplyButton(
  modifier: Modifier = Modifier,
  textRes: StringResource = Res.string.email_action_email_reply,
  onClick: () -> Unit = {},
) {
  FilledTonalButton(
    onClick = onClick,
    modifier = modifier,
    shapes = ButtonDefaults.shapes(),
  ) {
    Text(
      text = stringResource(textRes),
    )
  }
}
