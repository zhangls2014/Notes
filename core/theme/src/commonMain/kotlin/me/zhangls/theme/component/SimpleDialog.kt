package me.zhangls.theme.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview

/**
 * 纯文本内容的便捷重载，委托给 [AnnotatedString] 版本。
 *
 * 版式由下方重载中的 Material AlertDialog 统一管理 —— 两个重载各写一份
 * 主体时，改一次样式要记得改两处，漏改不会报错，只会让两种对话框长得不一样。
 *
 * @author zhangls
 */
@Composable
fun SimpleDialog(
  title: String,
  content: String,
  confirmText: String,
  onDismissRequest: () -> Unit,
  destructiveConfirm: Boolean = false,
  confirm: (() -> Unit)? = null,
  dismissText: String? = null,
  dismiss: (() -> Unit)? = null,
) {
  SimpleDialog(
    title = title,
    content = AnnotatedString(content),
    confirmText = confirmText,
    onDismissRequest = onDismissRequest,
    destructiveConfirm = destructiveConfirm,
    confirm = confirm,
    dismissText = dismissText,
    dismiss = dismiss,
  )
}

@Preview
@Composable
private fun SimpleDialogPreview() {
  SimpleDialog(
    title = "提示",
    content = "您确定要删除该文件吗？",
    confirmText = "确定",
    onDismissRequest = {},
    dismissText = "取消",
    confirm = {},
    dismiss = {},
  )
}

@Composable
fun SimpleDialog(
  title: String,
  content: AnnotatedString,
  confirmText: String,
  onDismissRequest: () -> Unit,
  destructiveConfirm: Boolean = false,
  confirm: (() -> Unit)? = null,
  dismissText: String? = null,
  dismiss: (() -> Unit)? = null,
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(text = title) },
    text = { Text(text = content) },
    confirmButton = {
      TextButton(onClick = { confirm?.invoke() }) {
        Text(
          text = confirmText,
          color = if (destructiveConfirm) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
      }
    },
    dismissButton = dismissText?.let { label ->
      {
        TextButton(onClick = { dismiss?.invoke() }) {
          Text(text = label)
        }
      }
    },
  )
}

@Preview
@Composable
private fun SimpleAnnotatedDialogPreview() {
  SimpleDialog(
    title = "提示",
    content = buildAnnotatedString {
      append("您确定要删除")
      withLink(
        LinkAnnotation.Clickable(
          tag = "privacy_link",
          styles = TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary)),
          linkInteractionListener = {},
        ),
      ) {
        append("用户协议")
      }
      append("文件吗")
    },
    confirmText = "确定",
    onDismissRequest = {},
    dismissText = "取消",
    confirm = {},
    dismiss = {},
  )
}
