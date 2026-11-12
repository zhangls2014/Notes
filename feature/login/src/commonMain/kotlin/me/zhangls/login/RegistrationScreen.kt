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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.zhangls.login.api.LocalAccountRegistration
import me.zhangls.theme.component.AdaptiveContent
import me.zhangls.theme.component.CenteredTopAppBar
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
import notes.feature.login.generated.resources.signup_create_failed
import notes.feature.login.generated.resources.signup_credentials
import notes.feature.login.generated.resources.signup_email
import notes.feature.login.generated.resources.signup_password_hint
import notes.feature.login.generated.resources.signup_resend_code
import notes.feature.login.generated.resources.signup_send_code
import notes.feature.login.generated.resources.signup_send_failed
import notes.feature.login.generated.resources.signup_title
import notes.feature.login.generated.resources.signup_username
import notes.feature.login.generated.resources.signup_username_hint
import notes.feature.login.generated.resources.signup_verify_email
import notes.feature.login.generated.resources.signup_verify_failed
import notes.feature.login.generated.resources.signup_working
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val CODE_LENGTH = 6
private const val RESEND_SECONDS = 60
private const val SECOND_MS = 1000L
private const val MIN_PASSWORD_LENGTH = 15
private val usernamePattern = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{2,63}")

@Composable
internal fun RegistrationScreen(onBack: () -> Unit, onRegistered: () -> Unit) {
  val auth = LocalAccountRegistration.current ?: return
  val scope = rememberCoroutineScope()
  var email by remember { mutableStateOf("") }
  var code by remember { mutableStateOf("") }
  var sentEmail by remember { mutableStateOf<String?>(null) }
  var proof by remember { mutableStateOf<String?>(null) }
  var resendIn by remember { mutableIntStateOf(0) }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<StringResource?>(null) }

  fun launch(failureMessage: StringResource, action: suspend () -> Unit) {
    if (busy) return
    busy = true
    error = null
    scope.launch {
      try {
        action()
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        error = failureMessage
      } finally {
        busy = false
      }
    }
  }

  fun backToEmail() {
    proof = null
    code = ""
    error = null
  }

  LaunchedEffect(resendIn) {
    if (resendIn > 0) {
      delay(SECOND_MS)
      resendIn--
    }
  }

  val direction = LocalLayoutDirection.current
  Scaffold(topBar = {
    CenteredTopAppBar(
      title = stringResource(Res.string.signup_title),
      navigate = {
        if (!busy) {
          if (proof == null) onBack() else backToEmail()
        }
      },
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        val verified = proof
        if (verified == null) {
          VerifyEmailForm(
            email = email,
            onEmailChange = { email = it },
            code = code,
            onCodeChange = { code = it.filter { char -> char in '0'..'9' }.take(CODE_LENGTH) },
            codeSent = sentEmail == email.trim(),
            resendIn = resendIn,
            enabled = !busy,
            onSend = {
              launch(Res.string.signup_send_failed) {
                val recipient = email.trim()
                auth.sendSignupCode(recipient)
                sentEmail = recipient
                code = ""
                resendIn = RESEND_SECONDS
              }
            },
            onVerify = {
              launch(Res.string.signup_verify_failed) { proof = auth.verifySignupCode(email.trim(), code) }
            },
          )
        } else {
          SignupCredentials(
            enabled = !busy,
            onBack = ::backToEmail,
            onCreate = { username, password ->
              launch(Res.string.signup_create_failed) {
                auth.registerAccount(email.trim(), verified, username.trim(), password)
                onRegistered()
              }
            },
          )
        }
        if (busy) Text(stringResource(Res.string.signup_working))
        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
      }
    }
  }
}

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
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(
      value = email,
      onValueChange = onEmailChange,
      label = { Text(stringResource(Res.string.signup_email)) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
      modifier = Modifier.weight(1f),
      enabled = enabled,
      singleLine = true,
    )
    FilledTonalButton(onClick = onSend, enabled = enabled && email.isNotBlank() && resendIn == 0) {
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
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    modifier = Modifier.fillMaxWidth(),
    enabled = enabled,
    singleLine = true,
  )
  Button(onClick = onVerify, enabled = enabled && codeSent && code.length == CODE_LENGTH, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(Res.string.signup_continue))
  }
}

@Composable
private fun SignupCredentials(enabled: Boolean, onBack: () -> Unit, onCreate: (String, String) -> Unit) {
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var visible by remember { mutableStateOf(false) }
  Text(stringResource(Res.string.signup_credentials), style = MaterialTheme.typography.titleLarge)
  OutlinedTextField(
    value = username,
    onValueChange = { username = it },
    label = { Text(stringResource(Res.string.signup_username)) },
    supportingText = { Text(stringResource(Res.string.signup_username_hint)) },
    modifier = Modifier.fillMaxWidth(),
    enabled = enabled,
    singleLine = true,
  )
  OutlinedTextField(
    value = password,
    onValueChange = { password = it },
    label = { Text(stringResource(Res.string.login_label_password)) },
    supportingText = { Text(stringResource(Res.string.signup_password_hint)) },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
    trailingIcon = {
      IconButton(onClick = { visible = !visible }) {
        Icon(
          imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
          contentDescription = stringResource(
            if (visible) Res.string.login_action_hide_password else Res.string.login_action_show_password,
          ),
        )
      }
    },
    modifier = Modifier.fillMaxWidth(),
    enabled = enabled,
    singleLine = true,
  )
  Button(
    onClick = { onCreate(username, password) },
    enabled = enabled && usernamePattern.matches(username.trim()) && password.count { !it.isLowSurrogate() } >= MIN_PASSWORD_LENGTH,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(stringResource(Res.string.signup_create))
  }
  TextButton(onClick = onBack, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
    Text(stringResource(Res.string.signup_back_to_email))
  }
}
