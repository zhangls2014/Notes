package me.zhangls.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import me.zhangls.login.api.LocalAccountRegistration
import me.zhangls.login.domain.LoginValidator
import me.zhangls.login.domain.text
import me.zhangls.login.mvi.RegistrationViewModel
import me.zhangls.theme.component.AdaptiveContent
import me.zhangls.theme.component.CenteredTopAppBar
import me.zhangls.theme.component.ContainedLoadingIndicator
import me.zhangls.theme.component.RoundedAutofillHighlight
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Visibility
import me.zhangls.theme.icon.VisibilityOff
import me.zhangls.theme.layout.ContentWidth
import notes.feature.login.generated.resources.Res
import notes.feature.login.generated.resources.login_action_hide_password
import notes.feature.login.generated.resources.login_action_show_password
import notes.feature.login.generated.resources.login_label_password
import notes.feature.login.generated.resources.signup_back_to_email
import notes.feature.login.generated.resources.signup_code
import notes.feature.login.generated.resources.signup_continue
import notes.feature.login.generated.resources.signup_create
import notes.feature.login.generated.resources.signup_credentials
import notes.feature.login.generated.resources.signup_email
import notes.feature.login.generated.resources.signup_password_hint
import notes.feature.login.generated.resources.signup_resend_code
import notes.feature.login.generated.resources.signup_send_code
import notes.feature.login.generated.resources.signup_title
import notes.feature.login.generated.resources.signup_username
import notes.feature.login.generated.resources.signup_username_hint
import notes.feature.login.generated.resources.signup_verify_email
import notes.feature.login.generated.resources.signup_working
import org.jetbrains.compose.resources.stringResource

private const val CODE_LENGTH = 6

@Composable
internal fun RegistrationScreen(
  onBack: () -> Unit,
  onRegistrationSuccess: () -> Unit,
  registration: RegistrationViewModel = viewModel { RegistrationViewModel() },
) {
  val auth = LocalAccountRegistration.current ?: return
  val currentOnRegistrationSuccess = rememberUpdatedState(onRegistrationSuccess)
  LaunchedEffect(registration.registered) {
    if (registration.registered) currentOnRegistrationSuccess.value()
  }
  ClearFocusWhileRegistering(registration.busy)

  RegistrationScaffold(onBack = {
    if (!registration.busy) {
      if (registration.emailVerified) registration.backToEmail() else onBack()
    }
  }) {
    if (!registration.emailVerified) {
      VerifyEmailForm(
        email = registration.email,
        onEmailChange = { registration.email = it },
        code = registration.code,
        onCodeChange = { registration.code = it.filter { char -> char in '0'..'9' }.take(CODE_LENGTH) },
        codeSent = registration.codeSent,
        resendIn = registration.resendIn,
        enabled = !registration.busy,
        onSend = { registration.sendCode(auth) },
        onVerify = { registration.verifyCode(auth) },
      )
    } else {
      SignupCredentials(
        username = registration.username,
        onUsernameChange = { registration.username = it },
        password = registration.password,
        onPasswordChange = { registration.password = it },
        passwordVisible = registration.passwordVisible,
        onTogglePasswordVisibility = { registration.passwordVisible = !registration.passwordVisible },
        enabled = !registration.busy,
        onBack = registration::backToEmail,
        onCreate = { registration.register(auth) },
      )
    }
    registration.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
  }

  if (registration.busy) {
    ContainedLoadingIndicator(label = stringResource(Res.string.signup_working))
  }
}

@Composable
private fun RegistrationScaffold(onBack: () -> Unit, content: @Composable () -> Unit) {
  val direction = LocalLayoutDirection.current
  Scaffold(topBar = {
    CenteredTopAppBar(
      title = stringResource(Res.string.signup_title),
      navigate = onBack,
    )
  }) { padding ->
    AdaptiveContent(
      modifier = Modifier.padding(
        start = padding.calculateStartPadding(direction),
        top = padding.calculateTopPadding(),
        end = padding.calculateEndPadding(direction),
      ).imePadding(),
      maxWidth = ContentWidth.Form,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp).padding(top = 16.dp, bottom = padding.calculateBottomPadding() + 24.dp),
        // 与登录页一致：字段之间 16dp，字段到主按钮 32dp（spacedBy + 按钮自身 top 16dp），
        // 主按钮到次级按钮 16dp。
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        content()
      }
    }
  }
}

@Composable
private fun ClearFocusWhileRegistering(busy: Boolean) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current
  SideEffect(busy) {
    if (busy) {
      focusManager.clearFocus(force = true)
      keyboardController?.hide()
    }
  }
}

/** 与登录页输入框一致的描边颜色：未聚焦时用更浅的分隔线色。 */
@Composable
private fun signupFieldColors() =
  OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun VerifyEmailForm(
  email: String,
  onEmailChange: (String) -> Unit,
  code: String,
  onCodeChange: (String) -> Unit,
  codeSent: Boolean,
  resendIn: Int,
  enabled: Boolean,
  onSend: () -> Unit,
  onVerify: () -> Unit,
) {
  Text(stringResource(Res.string.signup_verify_email), style = MaterialTheme.typography.titleLarge)
  // The outline starts below the space reserved for half of the floating label.
  val labelLineHeight = MaterialTheme.typography.bodySmall.lineHeight.takeIf { it.isSp } ?: 16.sp
  val labelInset = with(LocalDensity.current) { labelLineHeight.toDp() / 2 }
  RoundedAutofillHighlight(MaterialTheme.shapes.large) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(Res.string.signup_email)) },
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.large,
        colors = signupFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.weight(1f),
        enabled = enabled,
        singleLine = true,
      )
      FilledTonalButton(
        onClick = onSend,
        enabled = enabled && email.isNotBlank() && resendIn == 0,
        modifier = Modifier.padding(top = labelInset),
      ) {
        Text(
          if (resendIn > 0) {
            stringResource(Res.string.signup_resend_code, resendIn)
          } else {
            stringResource(Res.string.signup_send_code)
          },
        )
      }
    }
    OutlinedTextField(
      value = code,
      onValueChange = onCodeChange,
      label = { Text(stringResource(Res.string.signup_code)) },
      textStyle = MaterialTheme.typography.bodyLarge,
      shape = MaterialTheme.shapes.large,
      colors = signupFieldColors(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      modifier = Modifier.fillMaxWidth(),
      enabled = enabled,
      singleLine = true,
    )
  }
  Button(
    onClick = onVerify,
    enabled = enabled && codeSent && code.length == CODE_LENGTH,
    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
  ) {
    Text(stringResource(Res.string.signup_continue))
  }
}

@Composable
private fun SignupCredentials(
  username: String,
  onUsernameChange: (String) -> Unit,
  password: String,
  onPasswordChange: (String) -> Unit,
  passwordVisible: Boolean,
  onTogglePasswordVisibility: () -> Unit,
  enabled: Boolean,
  onBack: () -> Unit,
  onCreate: () -> Unit,
) {
  val accountError = LoginValidator.validateNewAccount(username)
  val passwordError = LoginValidator.validatePassword(password)
  val usernameHint = stringResource(Res.string.signup_username_hint)
  val passwordHint = stringResource(Res.string.signup_password_hint)
  val usernameMessage = if (username.isEmpty()) usernameHint else accountError.text().ifEmpty { usernameHint }
  val passwordMessage = if (password.isEmpty()) passwordHint else passwordError.text().ifEmpty { passwordHint }
  Text(stringResource(Res.string.signup_credentials), style = MaterialTheme.typography.titleLarge)
  OutlinedTextField(
    value = username,
    onValueChange = onUsernameChange,
    label = { Text(stringResource(Res.string.signup_username)) },
    textStyle = MaterialTheme.typography.bodyLarge,
    shape = MaterialTheme.shapes.large,
    colors = signupFieldColors(),
    isError = accountError != null && username.isNotEmpty(),
    supportingText = { Text(usernameMessage) },
    modifier = Modifier.fillMaxWidth(),
    enabled = enabled,
    singleLine = true,
  )
  OutlinedTextField(
    value = password,
    onValueChange = onPasswordChange,
    label = { Text(stringResource(Res.string.login_label_password)) },
    textStyle = MaterialTheme.typography.bodyLarge,
    shape = MaterialTheme.shapes.large,
    colors = signupFieldColors(),
    isError = passwordError != null && password.isNotEmpty(),
    supportingText = { Text(passwordMessage) },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
    trailingIcon = {
      IconButton(onClick = onTogglePasswordVisibility) {
        Icon(
          imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
          contentDescription = stringResource(
            if (passwordVisible) Res.string.login_action_hide_password else Res.string.login_action_show_password,
          ),
        )
      }
    },
    modifier = Modifier.fillMaxWidth(),
    enabled = enabled,
    singleLine = true,
  )
  Button(
    onClick = onCreate,
    enabled = enabled && accountError == null && passwordError == null,
    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
  ) {
    Text(stringResource(Res.string.signup_create))
  }
  TextButton(onClick = onBack, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(Res.string.signup_back_to_email))
  }
}
