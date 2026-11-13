package me.zhangls.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.component.GroupedDropdownMenu
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.MoreVert
import me.zhangls.theme.icon.Passkey
import me.zhangls.theme.layout.ContentWidth
import me.zhangls.theme.layout.contentWidth
import me.zhangls.profile.api.PasskeyManagement
import me.zhangls.profile.api.RegisteredPasskey
import me.zhangls.profile.mvi.PasskeyManagementViewModel
import notes.feature.profile.generated.resources.Res
import notes.feature.profile.generated.resources.auth_passkey_add
import notes.feature.profile.generated.resources.auth_passkey_brand_apple
import notes.feature.profile.generated.resources.auth_passkey_brand_unknown
import notes.feature.profile.generated.resources.auth_passkey_brand_xiaomi
import notes.feature.profile.generated.resources.auth_passkey_cancel
import notes.feature.profile.generated.resources.auth_passkey_created
import notes.feature.profile.generated.resources.auth_passkey_device
import notes.feature.profile.generated.resources.auth_passkey_empty
import notes.feature.profile.generated.resources.auth_passkey_error
import notes.feature.profile.generated.resources.auth_passkey_manage
import notes.feature.profile.generated.resources.auth_passkey_more
import notes.feature.profile.generated.resources.auth_passkey_provider_google
import notes.feature.profile.generated.resources.auth_passkey_provider_unknown
import notes.feature.profile.generated.resources.auth_passkey_remove
import notes.feature.profile.generated.resources.auth_passkey_remove_body
import notes.feature.profile.generated.resources.auth_passkey_remove_title
import notes.feature.profile.generated.resources.auth_passkey_retry
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PasskeyManagementScreen(
  management: PasskeyManagement,
  onBack: () -> Unit,
) {
  val viewModel = viewModel { PasskeyManagementViewModel(management) }
  val state by viewModel.state.collectAsStateWithLifecycle()

  val safePadding = WindowInsets.safeDrawing.asPaddingValues()
  val layoutDirection = LocalLayoutDirection.current
  val horizontalPadding = maxOf(
    safePadding.calculateLeftPadding(layoutDirection),
    safePadding.calculateRightPadding(layoutDirection),
  ) + 24.dp
  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical),
    topBar = { CenteredTopAppBar(title = stringResource(Res.string.auth_passkey_manage), navigate = onBack) },
    bottomBar = {
      if (management.canAdd) {
        Surface {
          Box(
            modifier = Modifier.fillMaxWidth()
              .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
              .padding(horizontal = horizontalPadding, vertical = 12.dp),
            contentAlignment = Alignment.Center,
          ) {
            Button(
              onClick = { viewModel.add(management::add) },
              modifier = Modifier.contentWidth(ContentWidth.Form),
              enabled = !state.busy,
            ) {
              Text(stringResource(Res.string.auth_passkey_add))
            }
          }
        }
      }
    },
  ) { padding ->
    val currentError = state.error
    val currentPasskeys = state.passkeys
    LazyColumn(
      modifier = Modifier.fillMaxSize()
        .padding(top = padding.calculateTopPadding())
        .consumeWindowInsets(padding),
      horizontalAlignment = Alignment.CenterHorizontally,
      contentPadding = PaddingValues(
        start = horizontalPadding,
        top = 16.dp,
        end = horizontalPadding,
        bottom = padding.calculateBottomPadding() + 16.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (currentError != null) {
        item {
          Surface(
            modifier = Modifier.contentWidth(ContentWidth.Form),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
          ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
              Text(stringResource(currentError), color = MaterialTheme.colorScheme.onErrorContainer)
              if (currentError == Res.string.auth_passkey_error) {
                TextButton(onClick = viewModel::reload, enabled = !state.busy) {
                  Text(stringResource(Res.string.auth_passkey_retry))
                }
              }
            }
          }
        }
      }
      if (currentPasskeys == null && currentError == null) {
        item {
          Box(
            Modifier.contentWidth(ContentWidth.Form).padding(32.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator()
          }
        }
      }
      if (currentPasskeys?.isEmpty() == true) {
        item {
          Column(
            Modifier.contentWidth(ContentWidth.Form).padding(vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(Icons.Rounded.Passkey, contentDescription = null, modifier = Modifier.size(40.dp))
            Text(stringResource(Res.string.auth_passkey_empty), style = MaterialTheme.typography.titleMedium)
          }
        }
      }
      items(currentPasskeys.orEmpty(), key = { it.id }) { passkey ->
        PasskeyRow(passkey, enabled = !state.busy, onRevoke = { viewModel.requestRevocation(passkey) })
      }
    }
  }

  if (state.pendingRevocation != null) {
    AlertDialog(
      onDismissRequest = viewModel::dismissRevocation,
      title = { Text(stringResource(Res.string.auth_passkey_remove_title)) },
      text = { Text(stringResource(Res.string.auth_passkey_remove_body)) },
      confirmButton = {
        TextButton(onClick = viewModel::confirmRevocation) {
          Text(stringResource(Res.string.auth_passkey_remove), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = viewModel::dismissRevocation) { Text(stringResource(Res.string.auth_passkey_cancel)) }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PasskeyRow(passkey: RegisteredPasskey, enabled: Boolean, onRevoke: () -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  val provider = when (passkey.provider) {
    "google_password_manager" -> Res.string.auth_passkey_provider_google
    else -> Res.string.auth_passkey_provider_unknown
  }
  val deviceBrand = when (passkey.deviceBrand?.lowercase()) {
    "xiaomi" -> stringResource(Res.string.auth_passkey_brand_xiaomi)
    "apple" -> stringResource(Res.string.auth_passkey_brand_apple)
    else -> passkey.deviceBrand?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.auth_passkey_brand_unknown)
  }
  Surface(
    modifier = Modifier.contentWidth(ContentWidth.Form).testTag("passkey-row"),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
          Icon(Icons.Rounded.Passkey, contentDescription = null)
        }
      }
      Column(Modifier.weight(1f)) {
        Text(stringResource(provider), style = MaterialTheme.typography.bodyLarge)
        Text(
          stringResource(Res.string.auth_passkey_created, passkey.createdAt.substringBefore('T')),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          stringResource(Res.string.auth_passkey_device, deviceBrand),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Box {
        IconButton(onClick = { expanded = true }, enabled = enabled) {
          Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(Res.string.auth_passkey_more))
        }
        GroupedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          DropdownMenuItem(
            text = { Text(stringResource(Res.string.auth_passkey_remove)) },
            shape = MenuDefaults.itemShapes().selectedShape,
            onClick = {
              expanded = false
              onRevoke()
            },
          )
        }
      }
    }
  }
}
