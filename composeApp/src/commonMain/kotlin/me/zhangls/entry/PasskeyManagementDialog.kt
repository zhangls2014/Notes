package me.zhangls.entry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import me.zhangls.login.api.PasskeyCreationTimedOutException
import me.zhangls.login.api.PasskeyCreationUnavailableException
import notes.composeapp.generated.resources.Res
import notes.composeapp.generated.resources.auth_passkey_add
import notes.composeapp.generated.resources.auth_passkey_close
import notes.composeapp.generated.resources.auth_passkey_empty
import notes.composeapp.generated.resources.auth_passkey_error
import notes.composeapp.generated.resources.auth_passkey_manage
import notes.composeapp.generated.resources.auth_passkey_no_service
import notes.composeapp.generated.resources.auth_passkey_recent
import notes.composeapp.generated.resources.auth_passkey_remove
import notes.composeapp.generated.resources.auth_passkey_retry
import notes.composeapp.generated.resources.auth_passkey_timeout
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PasskeyManagementDialog(
  listPasskeys: suspend () -> List<RegisteredPasskey>,
  addPasskey: (suspend () -> Boolean)?,
  revokePasskey: suspend (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  var passkeys by remember { mutableStateOf<List<RegisteredPasskey>?>(null) }
  var error by remember { mutableStateOf<StringResource?>(null) }
  var busy by remember { mutableStateOf(false) }

  suspend fun update(operation: suspend () -> Unit) {
    error = null
    try {
      operation()
      passkeys = listPasskeys()
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: PasskeyCreationUnavailableException) {
      error = Res.string.auth_passkey_no_service
    } catch (_: PasskeyCreationTimedOutException) {
      error = Res.string.auth_passkey_timeout
    } catch (failure: AuthHttpException) {
      error = if (failure.status == 403) Res.string.auth_passkey_recent else Res.string.auth_passkey_error
    } catch (_: Exception) {
      error = Res.string.auth_passkey_error
    } finally {
      busy = false
    }
  }

  fun submit(operation: suspend () -> Unit) {
    if (busy) return
    busy = true
    scope.launch { update(operation) }
  }

  LaunchedEffect(listPasskeys) { submit {} }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.auth_passkey_manage)) },
    text = {
      Column(Modifier.verticalScroll(rememberScrollState())) {
        error?.let { Text(stringResource(it)) }
        if (passkeys == null && error != null) {
          TextButton(onClick = { submit {} }, enabled = !busy) {
            Text(stringResource(Res.string.auth_passkey_retry))
          }
        }
        if (passkeys?.isEmpty() == true) Text(stringResource(Res.string.auth_passkey_empty))
        passkeys.orEmpty().forEach { passkey ->
          Row {
            Text(passkey.createdAt.substringBefore('T'), Modifier.weight(1f))
            TextButton(enabled = !busy, onClick = { submit { revokePasskey(passkey.id) } }) {
              Text(stringResource(Res.string.auth_passkey_remove))
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(Res.string.auth_passkey_close)) }
    },
    dismissButton = addPasskey?.let { add ->
      {
        TextButton(enabled = !busy, onClick = { submit { add() } }) {
          Text(stringResource(Res.string.auth_passkey_add))
        }
      }
    },
  )
}
