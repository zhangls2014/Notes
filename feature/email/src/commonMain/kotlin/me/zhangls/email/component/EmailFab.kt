package me.zhangls.email.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.zhangls.theme.icon.Edit
import me.zhangls.theme.icon.Icons
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_action_new_email
import notes.feature.email.generated.resources.email_action_sending
import org.jetbrains.compose.resources.stringResource

/**
 * 写邮件悬浮按钮：发送中展示进度并禁用点击。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun NewEmailFab(
  isSending: Boolean,
  listState: LazyListState,
  onOpenDraft: () -> Unit,
) {
  // LazyListState 只恢复位置，不恢复滚动方向；布局前 canScrollBackward 也为 false。
  // 单独恢复展开状态，避免切回 Tab 时先展开再收起。
  var expanded by rememberSaveable { mutableStateOf(true) }
  LaunchedEffect(listState) {
    snapshotFlow {
      if (listState.isScrollInProgress &&
        (listState.lastScrolledForward || listState.lastScrolledBackward)
      ) {
        listState.lastScrolledBackward || !listState.canScrollBackward
      } else {
        null
      }
    }.collect { scrollingExpanded ->
      if (scrollingExpanded != null) expanded = scrollingExpanded
    }
  }
  ExtendedFloatingActionButton(
    text = {
      Text(
        text = if (isSending) {
          stringResource(Res.string.email_action_sending)
        } else {
          stringResource(Res.string.email_action_new_email)
        }
      )
    },
    icon = {
      if (isSending) {
        LoadingIndicator(
          modifier = Modifier.size(24.dp),
        )
      } else {
        Icon(
          imageVector = Icons.Rounded.Edit,
          contentDescription = stringResource(Res.string.email_action_new_email)
        )
      }
    },
    onClick = { if (!isSending) onOpenDraft() },
    expanded = expanded,
  )
}
