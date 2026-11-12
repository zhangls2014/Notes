package me.zhangls.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import notes.feature.profile.generated.resources.Res
import notes.feature.profile.generated.resources.profile_cancel
import notes.feature.profile.generated.resources.profile_change_email
import notes.feature.profile.generated.resources.profile_change_failed
import notes.feature.profile.generated.resources.profile_confirm_change
import notes.feature.profile.generated.resources.profile_current_password
import notes.feature.profile.generated.resources.profile_email_code
import notes.feature.profile.generated.resources.profile_email_hint
import notes.feature.profile.generated.resources.profile_label_username
import notes.feature.profile.generated.resources.profile_new_email
import notes.feature.profile.generated.resources.profile_resend_code
import notes.feature.profile.generated.resources.profile_save
import notes.feature.profile.generated.resources.profile_send_code
import notes.feature.profile.generated.resources.profile_send_failed
import notes.feature.profile.generated.resources.profile_set_username
import notes.feature.profile.generated.resources.profile_username_hint
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal enum class ProfileField { Username, Email }

private const val CODE_LENGTH = 6
private const val RESEND_SECONDS = 60
private const val SECOND_MS = 1000L

@Composable
internal fun ProfileIdentityEditor(
  field: ProfileField,
  username: String,
  onCancel: () -> Unit,
  onSaveUsername: suspend (String) -> Unit,
  onSendCode: suspend (String, String) -> Unit,
  onSaveEmail: suspend (String, String) -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    key(field) {
      when (field) {
        ProfileField.Username -> UsernameForm(username, onCancel, onSaveUsername)
        ProfileField.Email -> EmailForm(onCancel, onSendCode, onSaveEmail)
      }
    }
  }
}

@Composable
private fun UsernameForm(username: String, onCancel: () -> Unit, onSave: suspend (String) -> Unit) {
  var value by remember { mutableStateOf(username) }
  val operation = rememberProfileOperation()
  Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(stringResource(Res.string.profile_set_username), style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
      value = value,
      onValueChange = { value = it },
      label = { Text(stringResource(Res.string.profile_label_username)) },
      supportingText = { Text(stringResource(Res.string.profile_username_hint)) },
      singleLine = true,
      enabled = !operation.busy,
      modifier = Modifier.fillMaxWidth(),
    )
    EditFeedback(operation.error)
    EditActions(
      enabled = !operation.busy,
      canSave = value.isNotBlank() && value.trim() != username,
      label = Res.string.profile_save,
      onCancel = onCancel,
      onSave = {
        operation.submit(Res.string.profile_change_failed) {
          onSave(value)
          onCancel()
        }
      },
    )
  }
}

@Composable
private fun EmailForm(
  onCancel: () -> Unit,
  onSend: suspend (String, String) -> Unit,
  onSave: suspend (String, String) -> Unit,
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var code by remember { mutableStateOf("") }
  var sentEmail by remember { mutableStateOf<String?>(null) }
  var resendIn by remember { mutableIntStateOf(0) }
  val operation = rememberProfileOperation()

  LaunchedEffect(resendIn) {
    if (resendIn > 0) {
      delay(SECOND_MS)
      resendIn--
    }
  }
  Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(stringResource(Res.string.profile_change_email), style = MaterialTheme.typography.titleLarge)
    CurrentPasswordField(password, onChange = { password = it }, enabled = !operation.busy)
    EmailAddressField(
      email = email,
      onEmailChange = { email = it },
      enabled = !operation.busy,
      canSend = password.isNotEmpty() && email.isNotBlank() && resendIn == 0,
      resendIn = resendIn,
      onSend = {
        operation.submit(Res.string.profile_send_failed) {
          val recipient = email.trim()
          onSend(recipient, password)
          sentEmail = recipient
          password = ""
          code = ""
          resendIn = RESEND_SECONDS
        }
      },
    )
    OutlinedTextField(
      value = code,
      onValueChange = { code = it.filter { char -> char in '0'..'9' }.take(CODE_LENGTH) },
      label = { Text(stringResource(Res.string.profile_email_code)) },
      supportingText = { Text(stringResource(Res.string.profile_email_hint)) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      enabled = !operation.busy,
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
    EditFeedback(operation.error)
    EditActions(
      enabled = !operation.busy,
      canSave = sentEmail == email.trim() && code.length == CODE_LENGTH,
      label = Res.string.profile_confirm_change,
      onCancel = onCancel,
      onSave = {
        operation.submit(Res.string.profile_change_failed) {
          onSave(email.trim(), code)
          onCancel()
        }
      },
    )
  }
}

@Composable
private fun CurrentPasswordField(password: String, onChange: (String) -> Unit, enabled: Boolean) {
  OutlinedTextField(
    value = password,
    onValueChange = onChange,
    label = { Text(stringResource(Res.string.profile_current_password)) },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    visualTransformation = PasswordVisualTransformation(),
    enabled = enabled,
    singleLine = true,
    modifier = Modifier.fillMaxWidth(),
  )
}

@Composable
private fun EmailAddressField(
  email: String,
  onEmailChange: (String) -> Unit,
  enabled: Boolean,
  canSend: Boolean,
  resendIn: Int,
  onSend: () -> Unit,
) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    OutlinedTextField(
      value = email,
      onValueChange = onEmailChange,
      label = { Text(stringResource(Res.string.profile_new_email)) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
      enabled = enabled,
      singleLine = true,
      modifier = Modifier.weight(1f),
    )
    FilledTonalButton(onClick = onSend, enabled = enabled && canSend) {
      Text(
        if (resendIn > 0) {
          stringResource(Res.string.profile_resend_code, resendIn)
        } else {
          stringResource(Res.string.profile_send_code)
        },
      )
    }
  }
}

@Composable
private fun EditFeedback(error: StringResource?) {
  error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun EditActions(
  enabled: Boolean,
  canSave: Boolean,
  label: StringResource,
  onCancel: () -> Unit,
  onSave: () -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
    TextButton(onClick = onCancel, enabled = enabled) { Text(stringResource(Res.string.profile_cancel)) }
    Button(onClick = onSave, enabled = enabled && canSave) { Text(stringResource(label)) }
  }
}

private class ProfileOperation(private val scope: kotlinx.coroutines.CoroutineScope) {
  var busy by mutableStateOf(false)
    private set
  var error by mutableStateOf<StringResource?>(null)
    private set

  fun submit(failure: StringResource, action: suspend () -> Unit) {
    if (busy) return
    busy = true
    error = null
    scope.launch {
      try {
        action()
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        error = failure
      } finally {
        busy = false
      }
    }
  }
}

@Composable
private fun rememberProfileOperation(): ProfileOperation {
  val scope = rememberCoroutineScope()
  return remember(scope) { ProfileOperation(scope) }
}
