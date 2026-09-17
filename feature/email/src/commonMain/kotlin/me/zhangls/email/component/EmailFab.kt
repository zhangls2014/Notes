package me.zhangls.email.component

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onClick = { if (!isSending) onOpenDraft() },
    expanded = listState.lastScrolledBackward || listState.canScrollBackward.not(),
  )
}
