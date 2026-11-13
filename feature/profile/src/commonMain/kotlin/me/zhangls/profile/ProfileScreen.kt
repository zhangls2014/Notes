package me.zhangls.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import me.zhangls.profile.mvi.AvatarSaveStatus
import me.zhangls.profile.mvi.ProfileField
import me.zhangls.profile.mvi.ProfileIntent
import me.zhangls.profile.mvi.ProfileState
import me.zhangls.profile.mvi.ProfileViewModel
import me.zhangls.theme.component.AdaptiveContent
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.component.UserAvatar
import me.zhangls.theme.icon.AccountCircle
import me.zhangls.theme.icon.ArrowBackIosNew
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Mail
import me.zhangls.theme.layout.ContentWidth
import notes.feature.profile.generated.resources.Res
import notes.feature.profile.generated.resources.profile_action_change_avatar
import notes.feature.profile.generated.resources.profile_email
import notes.feature.profile.generated.resources.profile_email_unavailable
import notes.feature.profile.generated.resources.profile_label_avatar
import notes.feature.profile.generated.resources.profile_label_profile
import notes.feature.profile.generated.resources.profile_label_username
import notes.feature.profile.generated.resources.profile_msg_avatar_saved
import notes.feature.profile.generated.resources.profile_msg_save_avatar_failed
import notes.feature.profile.generated.resources.profile_msg_saving_avatar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val FORWARD_CHEVRON_ROTATION = 180f

@Composable
internal fun ProfileScreen(onBack: () -> Unit, viewModel: ProfileViewModel = koinViewModel()) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val picker = rememberFilePickerLauncher(
    type = FilePickerFileType.Image,
    selectionMode = FilePickerSelectionMode.Single,
    onResult = { viewModel.sendIntent(ProfileIntent.ChangeAvatar(it.firstOrNull())) },
  )
  val canEdit = state.user?.authOrigin != null
  ProfileContent(
    state,
    onBack = { if (state.edit == null) onBack() else viewModel.sendIntent(ProfileIntent.CloseEdit) },
    onChangeAvatar = { picker.launch() },
    onEditUsername = { viewModel.sendIntent(ProfileIntent.OpenEdit(ProfileField.Username)) }.takeIf { canEdit },
    onEditEmail = { viewModel.sendIntent(ProfileIntent.OpenEdit(ProfileField.Email)) }.takeIf { canEdit },
  ) {
    AnimatedProfileIdentityEditor(state.edit, viewModel::sendIntent)
  }
}

@Composable
internal fun ProfileContent(
  state: ProfileState,
  onBack: () -> Unit,
  onChangeAvatar: () -> Unit,
  onEditUsername: (() -> Unit)? = null,
  onEditEmail: (() -> Unit)? = null,
  editor: @Composable () -> Unit = {},
) {
  val direction = LocalLayoutDirection.current
  Scaffold(topBar = {
    CenteredTopAppBar(title = stringResource(Res.string.profile_label_profile), navigate = onBack)
  }) { padding ->
    AdaptiveContent(
      modifier = Modifier.padding(
        start = padding.calculateStartPadding(direction),
        top = padding.calculateTopPadding(),
        end = padding.calculateEndPadding(direction),
      ).imePadding().verticalScroll(rememberScrollState()),
      maxWidth = ContentWidth.Form,
    ) {
      ProfileDetails(
        state = state,
        onChangeAvatar = onChangeAvatar,
        onEditUsername = onEditUsername,
        onEditEmail = onEditEmail,
        padding = PaddingValues(bottom = padding.calculateBottomPadding()),
        editor = editor,
      )
    }
  }
}

@Composable
private fun ProfileDetails(
  state: ProfileState,
  onChangeAvatar: () -> Unit,
  onEditUsername: (() -> Unit)?,
  onEditEmail: (() -> Unit)?,
  padding: PaddingValues,
  editor: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(padding).padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    UserAvatar(
      path = state.user?.avatar,
      contentDescription = stringResource(Res.string.profile_label_avatar),
      modifier = Modifier.size(112.dp),
    )
    FilledTonalButton(
      onClick = onChangeAvatar,
      modifier = Modifier.heightIn(min = 48.dp),
      enabled = state.user != null && state.saveStatus != AvatarSaveStatus.Saving,
    ) {
      Text(stringResource(Res.string.profile_action_change_avatar))
    }
    ProfileFields(state, onEditUsername, onEditEmail)
    editor()
    val status = when (state.saveStatus) {
      AvatarSaveStatus.Idle -> null
      AvatarSaveStatus.Saving -> Res.string.profile_msg_saving_avatar
      AvatarSaveStatus.Saved -> Res.string.profile_msg_avatar_saved
      AvatarSaveStatus.Failed -> Res.string.profile_msg_save_avatar_failed
    }
    status?.let {
      Text(
        text = stringResource(it),
        color = if (state.saveStatus == AvatarSaveStatus.Failed) {
          MaterialTheme.colorScheme.error
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
      )
    }
  }
}

@Composable
private fun ProfileFields(state: ProfileState, onEditUsername: (() -> Unit)?, onEditEmail: (() -> Unit)?) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    ProfileFieldRow(
      label = stringResource(Res.string.profile_label_username),
      value = state.user?.nickname.orEmpty(),
      icon = Icons.Rounded.AccountCircle,
      onEdit = onEditUsername,
      enabled = state.edit?.busy != true,
    )
    if (state.user?.authOrigin != null) {
      ProfileFieldRow(
        label = stringResource(Res.string.profile_email),
        value = state.user.email ?: stringResource(Res.string.profile_email_unavailable),
        icon = Icons.Rounded.Mail,
        onEdit = onEditEmail,
        enabled = state.edit?.busy != true,
      )
    }
  }
}

@Composable
private fun ProfileFieldRow(
  label: String,
  value: String,
  icon: ImageVector,
  onEdit: (() -> Unit)?,
  enabled: Boolean,
) {
  val shape = MaterialTheme.shapes.large
  val color = MaterialTheme.colorScheme.surfaceContainerLow
  if (onEdit == null) {
    Surface(shape = shape, color = color) { ProfileFieldContent(label, value, icon, editable = false) }
  } else {
    Surface(onClick = onEdit, enabled = enabled, shape = shape, color = color) {
      ProfileFieldContent(label, value, icon, editable = true)
    }
  }
}

@Composable
private fun ProfileFieldContent(label: String, value: String, icon: ImageVector, editable: Boolean) {
  Row(
    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Icon(icon, contentDescription = null)
    Column(modifier = Modifier.weight(1f)) {
      Text(label, style = MaterialTheme.typography.titleSmall)
      if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.bodyMedium)
    }
    if (editable) {
      val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
      Icon(
        Icons.Rounded.ArrowBackIosNew,
        contentDescription = null,
        modifier = Modifier.size(16.dp).rotate(if (rtl) 0f else FORWARD_CHEVRON_ROTATION),
      )
    }
  }
}
