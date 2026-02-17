package me.zhangls.login

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import me.zhangls.framework.ext.withDebounce
import me.zhangls.framework.mvi.ToastEffect

/**
 * @author zhangls
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(viewmodel: LoginViewModel = hiltViewModel(), onLoginResult: (LoginResult) -> Unit) {
  val context = LocalContext.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val state by viewmodel.state.collectAsState()

  LaunchedEffect(Unit) {
    viewmodel.effect.collect { effect ->
      when (effect) {
        is ToastEffect -> Toast.makeText(context, effect.message, effect.duration()).show()
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
      AccountInput(
        account = state.account,
        accountError = state.accountError.orEmpty(),
        onAccountChange = { viewmodel.sendIntent(LoginIntent.UpdateAccount(it)) },
        onClearAccount = { viewmodel.sendIntent(LoginIntent.ClearAccount) }
      )

      PasswordInput(
        modifier = Modifier.padding(top = 16.dp),
        password = state.password,
        passwordError = state.passwordError.orEmpty(),
        passwordVisible = state.passwordVisible,
        onPasswordChange = { viewmodel.sendIntent(LoginIntent.UpdatePassword(it)) },
        onPasswordVisibleChange = { viewmodel.sendIntent(LoginIntent.UpdatePasswordVisible(it)) },
        onLogin = {
          keyboardController?.hide()
          viewmodel.sendIntent(LoginIntent.Login)
        }
      )

      Button(
        onClick = {
          keyboardController?.hide()
          viewmodel.sendIntent(LoginIntent.Login)
        }.withDebounce(),
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
}

@Composable
private fun AccountInput(
  modifier: Modifier = Modifier,
  account: String,
  accountError: String,
  onAccountChange: (String) -> Unit,
  onClearAccount: () -> Unit
) {
  val focusManager = LocalFocusManager.current

  TextField(
    value = account,
    onValueChange = { onAccountChange(it) },
    singleLine = true,
    isError = accountError.isNotEmpty(),
    textStyle = TextStyle.Default.copy(fontSize = 14.sp, fontFamily = FontFamily.Monospace),
    placeholder = { Text(text = stringResource(id = R.string.login_hint_login_account)) },
    modifier = modifier
      .padding(start = 32.dp, end = 32.dp, top = 16.dp)
      .fillMaxWidth(),
    leadingIcon = { Icon(imageVector = Icons.Rounded.AccountCircle, contentDescription = null) },
    trailingIcon = {
      if (account.isNotEmpty()) {
        Icon(
          imageVector = Icons.Default.Clear,
          contentDescription = null,
          modifier = Modifier.clickable { onClearAccount() }
        )
      }
    },
    supportingText = {
      Text(
        text = accountError,
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
  passwordError: String,
  passwordVisible: Boolean,
  onPasswordChange: (String) -> Unit,
  onPasswordVisibleChange: (Boolean) -> Unit,
  onLogin: () -> Unit
) {
  TextField(
    value = password,
    onValueChange = { onPasswordChange(it) },
    singleLine = true,
    isError = passwordError.isNotEmpty(),
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
        text = passwordError,
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