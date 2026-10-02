package me.zhangls.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import me.zhangls.profile.mvi.AvatarSaveStatus
import me.zhangls.profile.mvi.ProfileIntent
import me.zhangls.profile.mvi.ProfileState
import me.zhangls.profile.mvi.ProfileViewModel
import me.zhangls.theme.component.AdaptiveContent
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.component.UserAvatar
import notes.feature.profile.generated.resources.Res
import notes.feature.profile.generated.resources.profile_action_change_avatar
import notes.feature.profile.generated.resources.profile_label_avatar
import notes.feature.profile.generated.resources.profile_label_profile
import notes.feature.profile.generated.resources.profile_label_username
import notes.feature.profile.generated.resources.profile_msg_avatar_saved
import notes.feature.profile.generated.resources.profile_msg_save_avatar_failed
import notes.feature.profile.generated.resources.profile_msg_saving_avatar
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ProfileScreen(onBackPressed: () -> Unit, viewModel: ProfileViewModel = koinViewModel()) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val picker = rememberFilePickerLauncher(
    type = FilePickerFileType.Image,
    selectionMode = FilePickerSelectionMode.Single,
    onResult = { viewModel.sendIntent(ProfileIntent.ChangeAvatar(it.firstOrNull())) },
  )
  ProfileContent(state, onBackPressed, onChangeAvatar = { picker.launch() })
}

@Composable
internal fun ProfileContent(
  state: ProfileState,
  onBackPressed: () -> Unit,
  onChangeAvatar: () -> Unit,
) {
  Scaffold(
    topBar = {
      CenteredTopAppBar(
        title = stringResource(Res.string.profile_label_profile),
        navigate = onBackPressed,
      )
    },
  ) { padding ->
    AdaptiveContent(modifier = Modifier.padding(padding)) {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        UserAvatar(
          path = state.user?.avatar,
          contentDescription = stringResource(Res.string.profile_label_avatar),
          modifier = Modifier.size(112.dp),
        )
        Text(
          text = stringResource(Res.string.profile_label_username),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.semantics { heading() },
        )
        state.user?.let {
          Text(text = it.nickname, style = MaterialTheme.typography.bodyLarge)
        }
        Button(
          onClick = onChangeAvatar,
          modifier = Modifier.heightIn(min = 48.dp),
          enabled = state.user != null && state.saveStatus != AvatarSaveStatus.Saving,
        ) {
          Text(stringResource(Res.string.profile_action_change_avatar))
        }
        val status = when (state.saveStatus) {
          AvatarSaveStatus.Idle -> null
          AvatarSaveStatus.Saving -> Res.string.profile_msg_saving_avatar
          AvatarSaveStatus.Saved -> Res.string.profile_msg_avatar_saved
          AvatarSaveStatus.Failed -> Res.string.profile_msg_save_avatar_failed
        }
        status?.let {
          Text(
            text = stringResource(it),
            color = if (state.saveStatus == AvatarSaveStatus.Failed) MaterialTheme.colorScheme.error
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          )
        }
      }
    }
  }
}
