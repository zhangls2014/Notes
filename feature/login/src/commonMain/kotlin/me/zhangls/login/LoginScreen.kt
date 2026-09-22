package me.zhangls.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.login.mvi.LoginIntent
import me.zhangls.login.mvi.LoginViewModel
import me.zhangls.login.api.LoginResult
import me.zhangls.login.domain.AccountError
import me.zhangls.login.domain.PasswordError
import me.zhangls.login.domain.text
import me.zhangls.login.icon.AppLogo
import me.zhangls.preference.DarkThemePreference
import me.zhangls.preference.LanguagePreference
import me.zhangls.preference.ui.SelectIconButton
import me.zhangls.theme.component.ContainedLoadingIndicator
import me.zhangls.theme.icon.AccountCircle
import me.zhangls.theme.icon.Clear
import me.zhangls.theme.icon.Icons
import me.zhangls.theme.icon.Lock
import me.zhangls.theme.icon.Visibility
import me.zhangls.theme.icon.VisibilityOff
import notes.feature.login.generated.resources.Res
import notes.feature.login.generated.resources.login_action_login
import notes.feature.login.generated.resources.login_hint_login_account
import notes.feature.login.generated.resources.login_hint_login_password
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 表单最大宽度。Android 17 起大屏设备不再允许应用锁定方向/尺寸，窗口宽度可能达到上千 dp，
 * 这里对表单做宽度约束，避免单行输入框横跨整个窗口。
 */
private val FormMaxWidth = 480.dp

/**
 * @author zhangls
 */
@Composable
fun LoginScreen(viewModel: LoginViewModel = koinViewModel(), onLoginResult: (LoginResult) -> Unit) {
  val keyboardController = LocalSoftwareKeyboardController.current
  val state by viewModel.state.collectAsStateWithLifecycle()
  val loginClick = remember(keyboardController, viewModel) {
    {
      keyboardController?.hide()
      viewModel.sendIntent(LoginIntent.Login)
    }
  }

  LaunchedEffect(Unit) {
    viewModel.effect.collect { effect ->
      when (effect) {
        is LoginResult -> onLoginResult(effect)
      }
    }
  }

  Scaffold { padding ->
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(state = rememberScrollState())
        // 底部交给系统导航栏与键盘，不要把输入框压在它们下面。
        // imePadding 必须在 verticalScroll **之内**（即链尾）：它给滚动内容补上键盘高度的
        // 底部留白，于是滚动区域的内容变高，聚焦的输入框能被自动滚进键盘上方的可视区。
        // 全仓原先没有任何一处处理 IME，实测键盘会完全盖住下方输入框。
        .padding(bottom = padding.calculateBottomPadding())
        .imePadding()
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .align(alignment = Alignment.End)
          .padding(end = 16.dp, top = padding.calculateTopPadding() + 16.dp)
      ) {
        SelectIconButton(
          spec = DarkThemePreference.spec,
          value = state.darkTheme,
          onValueChange = { viewModel.sendIntent(LoginIntent.UpdateDarkTheme(it)) },
        )

        SelectIconButton(
          spec = LanguagePreference.spec,
          value = state.appLanguage,
          onValueChange = { viewModel.sendIntent(LoginIntent.UpdateLanguage(it)) },
        )
      }

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
          // 大屏（sw>=600dp）下不拉满整行，避免单行输入框宽到难以阅读
          .widthIn(max = FormMaxWidth)
          .padding(vertical = 32.dp)
      ) {
        Image(imageVector = AppLogo, contentDescription = null)

        AccountInput(
          modifier = Modifier.padding(top = 40.dp),
          account = state.account,
          accountError = state.accountError,
          onAccountChange = { viewModel.sendIntent(LoginIntent.UpdateAccount(it)) },
          onClearAccount = { viewModel.sendIntent(LoginIntent.ClearAccount) }
        )

        PasswordInput(
          modifier = Modifier.padding(top = 16.dp),
          password = state.password,
          passwordError = state.passwordError,
          passwordVisible = state.passwordVisible,
          onPasswordChange = { viewModel.sendIntent(LoginIntent.UpdatePassword(it)) },
          onPasswordVisibleChange = { viewModel.sendIntent(LoginIntent.UpdatePasswordVisible(it)) },
          onLogin = {
            keyboardController?.hide()
            viewModel.sendIntent(LoginIntent.Login)
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
          Text(text = stringResource(Res.string.login_action_login), fontSize = 16.sp)
        }
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
    placeholder = { Text(text = stringResource(Res.string.login_hint_login_account)) },
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
    placeholder = { Text(text = stringResource(Res.string.login_hint_login_password)) },
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

