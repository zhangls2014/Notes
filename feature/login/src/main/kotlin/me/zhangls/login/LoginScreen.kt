package me.zhangls.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.framework.ext.withDebounce
import me.zhangls.login.domain.AccountError
import me.zhangls.login.domain.PasswordError
import me.zhangls.login.domain.text
import me.zhangls.login.icon.AccountCircle
import me.zhangls.login.icon.AppLogo
import me.zhangls.login.icon.Clear
import me.zhangls.login.icon.Lock
import me.zhangls.login.icon.Visibility
import me.zhangls.login.icon.VisibilityOff
import me.zhangls.theme.component.ContainedLoadingIndicator
import me.zhangls.theme.icon.Icons
import org.koin.compose.viewmodel.koinViewModel

/**
 * @author zhangls
 */
@Composable
fun LoginScreen(viewmodel: LoginViewModel = koinViewModel(), onLoginResult: (LoginResult) -> Unit) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val state by viewmodel.state.collectAsStateWithLifecycle()
  val loginClick = remember(keyboardController, viewmodel) {
    {
      keyboardController?.hide()
      viewmodel.sendIntent(LoginIntent.Login)
    }.withDebounce()
  }

  LaunchedEffect(Unit) {
    viewmodel.effect.collect { effect ->
      when (effect) {
        is LoginResult -> onLoginResult(effect)
      }
    }
  }

  Scaffold { padding ->
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(state = rememberScrollState())
        .padding(padding)
    ) {
      Image(imageVector = AppLogo, contentDescription = null)

      AccountInput(
        modifier = Modifier.padding(top = 40.dp),
        account = state.account,
        accountError = state.accountError,
        onAccountChange = { viewmodel.sendIntent(LoginIntent.UpdateAccount(it)) },
        onClearAccount = { viewmodel.sendIntent(LoginIntent.ClearAccount) }
      )

      PasswordInput(
        modifier = Modifier.padding(top = 16.dp),
        password = state.password,
        passwordError = state.passwordError,
        passwordVisible = state.passwordVisible,
        onPasswordChange = { viewmodel.sendIntent(LoginIntent.UpdatePassword(it)) },
        onPasswordVisibleChange = { viewmodel.sendIntent(LoginIntent.UpdatePasswordVisible(it)) },
        onLogin = {
          keyboardController?.hide()
          viewmodel.sendIntent(LoginIntent.Login)
        }
      )

      Button(
        onClick = loginClick,
        enabled = state.isInputValid,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 32.dp, end = 32.dp, top = 32.dp)
      ) {
        Text(text = stringResource(id = R.string.login_action_login), fontSize = 16.sp)
      }
    }
  }

  if (state.isLoading) {
    ContainedLoadingIndicator()
  }
}

@Composable
private fun AccountInput(
  modifier: Modifier = Modifier,
  account: String,
  accountError: AccountError?,
  onAccountChange: (String) -> Unit,
  onClearAccount: () -> Unit
) {
  val focusManager = LocalFocusManager.current
  val inputError = accountError.text()

  TextField(
    value = account,
    onValueChange = { onAccountChange(it) },
    singleLine = true,
    isError = inputError.isNotEmpty(),
    textStyle = TextStyle.Default.copy(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
    placeholder = { Text(text = stringResource(id = R.string.login_hint_login_account)) },
    modifier = modifier
      .padding(start = 32.dp, end = 32.dp, top = 16.dp)
      .fillMaxWidth(),
    leadingIcon = { Icon(imageVector = Icons.Rounded.AccountCircle, contentDescription = null) },
    trailingIcon = {
      if (account.isNotEmpty()) {
        Icon(
          imageVector = Icons.Rounded.Clear,
          contentDescription = null,
          modifier = Modifier.clickable { onClearAccount() }
        )
      }
    },
    supportingText = {
      Text(
        text = inputError,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 36.dp)
      )
    },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
  )
}

@Composable
private fun PasswordInput(
  modifier: Modifier = Modifier,
  password: String,
  passwordError: PasswordError?,
  passwordVisible: Boolean,
  onPasswordChange: (String) -> Unit,
  onPasswordVisibleChange: (Boolean) -> Unit,
  onLogin: () -> Unit
) {
  val inputError = passwordError.text()

  TextField(
    value = password,
    onValueChange = { onPasswordChange(it) },
    singleLine = true,
    isError = inputError.isNotEmpty(),
    textStyle = TextStyle.Default.copy(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
    placeholder = { Text(text = stringResource(id = R.string.login_hint_login_password)) },
    modifier = modifier
      .padding(horizontal = 32.dp)
      .fillMaxWidth(),
    leadingIcon = { Icon(imageVector = Icons.Rounded.Lock, contentDescription = null) },
    trailingIcon = {
      Icon(
        imageVector = if (passwordVisible) {
          Icons.Rounded.VisibilityOff
        } else {
          Icons.Rounded.Visibility
        },
        contentDescription = null,
        modifier = Modifier.clickable { onPasswordVisibleChange(passwordVisible.not()) }
      )
    },
    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
    supportingText = {
      Text(
        text = inputError,
        fontSize = 12.sp,
        minLines = 2,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 36.dp)
      )
    },
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, keyboardType = KeyboardType.Password),
    keyboardActions = KeyboardActions(onGo = { onLogin() })
  )
}

@Preview
@Composable
fun LoginScreenPreview() {
  LoginScreen(onLoginResult = {})
}
