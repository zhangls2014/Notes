package me.zhangls.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInExpo
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import me.zhangls.profile.mvi.ProfileEdit
import me.zhangls.profile.mvi.ProfileEditError
import me.zhangls.profile.mvi.ProfileField
import me.zhangls.profile.mvi.ProfileIntent
import me.zhangls.theme.component.AccountTextField
import me.zhangls.theme.component.RoundedAutofillHighlight
import notes.feature.profile.generated.resources.Res
import notes.feature.profile.generated.resources.profile_cancel
import notes.feature.profile.generated.resources.profile_change_email
import notes.feature.profile.generated.resources.profile_change_failed
import notes.feature.profile.generated.resources.profile_change_username
import notes.feature.profile.generated.resources.profile_confirm_change
import notes.feature.profile.generated.resources.profile_current_password
import notes.feature.profile.generated.resources.profile_email_code
import notes.feature.profile.generated.resources.profile_incorrect_password
import notes.feature.profile.generated.resources.profile_label_username
import notes.feature.profile.generated.resources.profile_new_email
import notes.feature.profile.generated.resources.profile_save
import notes.feature.profile.generated.resources.profile_send_code
import notes.feature.profile.generated.resources.profile_send_failed
import notes.feature.profile.generated.resources.profile_verify_password
import notes.feature.profile.generated.resources.profile_verify_password_failed
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AnimatedProfileIdentityEditor(edit: ProfileEdit?, sendIntent: (ProfileIntent) -> Unit) {
  SlidingProfileContent(visible = edit != null) {
    // Keep the form's layout for the exit transition while releasing credentials immediately.
    var closingEdit by remember { mutableStateOf<ProfileEdit?>(null) }
    SideEffect {
      if (edit != null) closingEdit = edit.copy(password = "", code = "", busy = true)
    }
    (edit ?: closingEdit)?.let { ProfileIdentityEditor(it, sendIntent) }
  }
}

@Composable
internal fun ProfileIdentityEditor(edit: ProfileEdit, sendIntent: (ProfileIntent) -> Unit) {
  val isUsername = edit.field == ProfileField.Username
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(
        stringResource(if (isUsername) Res.string.profile_change_username else Res.string.profile_change_email),
        style = MaterialTheme.typography.titleLarge,
      )
      if (isUsername) {
        AccountTextField(
          value = edit.username,
          onValueChange = { sendIntent(ProfileIntent.UpdateUsername(it)) },
          label = { Text(stringResource(Res.string.profile_label_username)) },
          enabled = !edit.busy,
          modifier = Modifier.fillMaxWidth(),
        )
      } else {
        EmailFields(edit, sendIntent)
      }
      EditFeedback(edit.error)
      EditActions(edit, sendIntent)
    }
  }
}

@Composable
private fun EmailFields(edit: ProfileEdit, sendIntent: (ProfileIntent) -> Unit) {
  val passwordEnabled = !edit.busy && !edit.passwordVerified
  RoundedAutofillHighlight(MaterialTheme.shapes.large) {
    AccountTextField(
      value = edit.password,
      onValueChange = { sendIntent(ProfileIntent.UpdatePassword(it)) },
      label = { Text(stringResource(Res.string.profile_current_password)) },
      keyboardOptions = if (passwordEnabled) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
      visualTransformation = PasswordVisualTransformation(),
      isError = edit.error == ProfileEditError.IncorrectPassword,
      enabled = passwordEnabled,
      modifier = Modifier.fillMaxWidth().then(
        if (passwordEnabled) Modifier.semantics { contentType = ContentType.Password } else Modifier,
      ),
    )
  }
  EmailFieldVisibility(visible = edit.passwordVerified) {
    AccountTextField(
      value = edit.email,
      onValueChange = { sendIntent(ProfileIntent.UpdateEmail(it)) },
      label = { Text(stringResource(Res.string.profile_new_email)) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
      enabled = !edit.busy,
      modifier = Modifier.fillMaxWidth(),
    )
  }
  EmailFieldVisibility(visible = edit.codeRequested) {
    AccountTextField(
      value = edit.code,
      onValueChange = { sendIntent(ProfileIntent.UpdateCode(it)) },
      label = { Text(stringResource(Res.string.profile_email_code)) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      enabled = !edit.busy,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmailFieldVisibility(visible: Boolean, content: @Composable () -> Unit) {
  AnimatedVisibility(
    visible = visible,
    enter = expandVertically(MaterialTheme.motionScheme.defaultSpatialSpec(), expandFrom = Alignment.Top) +
      fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
    exit = ExitTransition.None,
    content = { content() },
  )
}

private const val SLIDE_ENTER_MILLIS = 300
private const val SLIDE_EXIT_MILLIS = 200

@Composable
private fun SlidingProfileContent(visible: Boolean, content: @Composable () -> Unit) {
  AnimatedVisibility(
    visible = visible,
    enter = expandVertically(tween(SLIDE_ENTER_MILLIS, easing = EaseOutExpo), expandFrom = Alignment.Top) +
      slideInVertically(tween(SLIDE_ENTER_MILLIS, easing = EaseOutExpo)) { -it / 3 },
    exit = shrinkVertically(tween(SLIDE_EXIT_MILLIS, easing = EaseInExpo), shrinkTowards = Alignment.Top) +
      slideOutVertically(tween(SLIDE_EXIT_MILLIS, easing = EaseInExpo)) { -it / 3 },
    content = { content() },
  )
}

@Composable
private fun EditFeedback(error: ProfileEditError?) {
  val message = when (error) {
    ProfileEditError.Save -> Res.string.profile_change_failed
    ProfileEditError.VerifyPassword -> Res.string.profile_verify_password_failed
    ProfileEditError.IncorrectPassword -> Res.string.profile_incorrect_password
    ProfileEditError.SendCode -> Res.string.profile_send_failed
    null -> return
  }
  Text(
    stringResource(message),
    color = MaterialTheme.colorScheme.error,
    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
  )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EditActions(edit: ProfileEdit, sendIntent: (ProfileIntent) -> Unit) {
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
  ) {
    TextButton(
      onClick = { sendIntent(ProfileIntent.CloseEdit) },
      enabled = !edit.busy,
    ) { Text(stringResource(Res.string.profile_cancel)) }
    Button(
      onClick = { sendIntent(ProfileIntent.ConfirmEdit) },
      enabled = edit.nextStep != null,
      shapes = ButtonDefaults.shapes(),
    ) {
      val label = when {
        edit.field == ProfileField.Username -> Res.string.profile_save
        !edit.passwordVerified -> Res.string.profile_verify_password
        !edit.codeRequested -> Res.string.profile_send_code
        else -> Res.string.profile_confirm_change
      }
      Text(stringResource(label))
    }
  }
}
