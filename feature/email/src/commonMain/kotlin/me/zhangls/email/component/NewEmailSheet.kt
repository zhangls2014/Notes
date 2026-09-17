package me.zhangls.email.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import me.zhangls.data.model.AccountModel
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.allDrawableResources
import notes.feature.email.generated.resources.email_action_cancel
import notes.feature.email.generated.resources.email_action_save
import notes.feature.email.generated.resources.email_label_body
import notes.feature.email.generated.resources.email_label_recipients
import notes.feature.email.generated.resources.email_label_subject
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun NewEmailSheet(
  recipients: List<AccountModel>,
  visible: Boolean,
  selectedRecipientIds: Set<Long>,
  subject: String,
  body: String,
  isSending: Boolean,
  onToggleRecipient: (Long, Boolean) -> Unit,
  onSubjectChange: (String) -> Unit,
  onBodyChange: (String) -> Unit,
  onDismiss: () -> Unit,
  onCancel: () -> Unit,
  onSave: () -> Unit,
) {
  if (!visible) return
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(onDismissRequest = { if (!isSending) onDismiss() }, sheetState = sheetState) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .verticalScroll(state = rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(Res.string.email_label_recipients),
        style = MaterialTheme.typography.titleMedium,
      )
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        recipients.forEach { recipient ->
          FilterChip(
            selected = selectedRecipientIds.contains(recipient.id),
            onClick = { onToggleRecipient(recipient.id, !selectedRecipientIds.contains(recipient.id)) },
            enabled = !isSending,
            leadingIcon = {
              Image(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape),
                painter = painterResource(Res.allDrawableResources.getValue(recipient.avatar)),
                contentScale = ContentScale.Crop,
                contentDescription = null,
              )
            },
            label = { Text(recipient.fullName) },
          )
        }
      }

      OutlinedTextField(
        value = subject,
        onValueChange = onSubjectChange,
        enabled = !isSending,
        label = { Text(text = stringResource(Res.string.email_label_subject)) },
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = body,
        onValueChange = onBodyChange,
        enabled = !isSending,
        label = { Text(text = stringResource(Res.string.email_label_body)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 4,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
      ) {
        TextButton(enabled = !isSending, onClick = onCancel) {
          Text(text = stringResource(Res.string.email_action_cancel))
        }
        Spacer(modifier = Modifier.size(8.dp))
        Button(enabled = !isSending, onClick = onSave) {
          Text(text = stringResource(Res.string.email_action_save))
        }
      }
    }
  }
}
