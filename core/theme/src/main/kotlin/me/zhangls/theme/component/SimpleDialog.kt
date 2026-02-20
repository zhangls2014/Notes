package me.zhangls.theme.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * @author zhangls
 */
@Composable
fun SimpleDialog(
  title: String,
  content: String,
  confirmText: String,
  confirm: (() -> Unit)? = null,
  dismissText: String? = null,
  dismiss: (() -> Unit)? = null,
) {
  Dialog(onDismissRequest = {}) {
    Column(
      modifier = Modifier
        .background(
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          shape = MaterialTheme.shapes.extraLarge
        )
    ) {
      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = title,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
        maxLines = 1,
        style = MaterialTheme.typography.titleLarge,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(16.dp))

      HorizontalDivider(thickness = 0.5.dp)

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = content,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
      )

      Spacer(modifier = Modifier.height(24.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.End
      ) {
        dismissText?.let {
          OutlinedButton(
            onClick = { dismiss?.invoke() },
            modifier = Modifier.weight(1F),
            border = BorderStroke(
              width = 1.dp,
              color = MaterialTheme.colorScheme.outline,
            )
          ) {
            Text(text = dismissText)
          }

          Spacer(modifier = Modifier.width(24.dp))
        }

        Button(
          onClick = { confirm?.invoke() },
          modifier = Modifier.weight(1F)
        ) {
          Text(text = confirmText)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Preview
@Composable
private fun SimpleDialogPreview() {
  SimpleDialog(
    title = "提示",
    content = "您确定要删除该文件吗？",
    confirmText = "确定",
    dismissText = "取消",
    confirm = {},
    dismiss = {}
  )
}

@Composable
fun SimpleDialog(
  title: String,
  content: AnnotatedString,
  confirmText: String,
  confirm: (() -> Unit)? = null,
  dismissText: String? = null,
  dismiss: (() -> Unit)? = null,
) {
  Dialog(onDismissRequest = {}) {
    Column(
      modifier = Modifier
        .background(
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          shape = MaterialTheme.shapes.extraLarge
        )
    ) {
      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = title,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
        maxLines = 1,
        style = MaterialTheme.typography.titleLarge,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(16.dp))

      HorizontalDivider(thickness = 0.5.dp)

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = content,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
      )

      Spacer(modifier = Modifier.height(24.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.End
      ) {
        dismissText?.let {
          OutlinedButton(
            onClick = { dismiss?.invoke() },
            modifier = Modifier.weight(1F),
            border = BorderStroke(
              width = 1.dp,
              color = MaterialTheme.colorScheme.outline,
            )
          ) {
            Text(text = dismissText)
          }

          Spacer(modifier = Modifier.width(24.dp))
        }

        Button(
          onClick = { confirm?.invoke() },
          modifier = Modifier.weight(1F)
        ) {
          Text(text = confirmText)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
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
          linkInteractionListener = {})
      ) {
        append("用户协议")
      }
      append("文件吗")
    },
    confirmText = "确定",
    dismissText = "取消",
    confirm = {},
    dismiss = {}
  )
}