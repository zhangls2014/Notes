package me.zhangls.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.login.api.LoginResult
import me.zhangls.login.domain.AccountError
import me.zhangls.login.domain.PasswordError
import me.zhangls.login.domain.text
import me.zhangls.login.icon.AppLogo
import me.zhangls.login.mvi.LoginIntent
import me.zhangls.login.mvi.LoginState
import me.zhangls.login.mvi.LoginViewModel
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
import me.zhangls.theme.layout.ContentWidth
import me.zhangls.theme.layout.LocalPaneScaffoldDirective
import me.zhangls.theme.layout.canShowSideBySidePanes
import me.zhangls.theme.layout.canShowStackedPanes
import me.zhangls.theme.layout.contentWidth
import notes.feature.login.generated.resources.Res
import notes.feature.login.generated.resources.login_action_login
import notes.feature.login.generated.resources.login_brand_name
import notes.feature.login.generated.resources.login_brand_tagline
import notes.feature.login.generated.resources.login_hint_login_account
import notes.feature.login.generated.resources.login_hint_login_password
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 登录页的三种排布。
 *
 * 注意这里**没有**宽度断点：判断依据来自 `PaneScaffoldDirective`（库按窗口形态算出的
 * 分区能力），本应用不自己维护"多少 dp 该分两栏"这套数字。
 */
internal enum class LoginArrangement {
  /** 单栏：品牌（logo）与表单叠在一列里，居中。 */
  Single,

  /** 横排：品牌区占左侧剩余空间，表单区占右侧且宽度封顶。 */
  SideBySide,

  /** 竖排：品牌区在上、表单区在下。 */
  Stacked,
}

/**
 * 由窗格排布指令决定登录页怎么摆。
 *
 * 顺序即优先级，三条都不是随手排的：
 *
 * 1. [LoginArrangement.Stacked] 优先 —— 能竖排只可能有两种情形：桌面支架姿态（横向铰链把
 *    窗口拦腰切断），或"窄而高"的窗口（只有一栏横向分区、高度却已到 Expanded）。
 *    让内容跨过铰链，比少用一点横向空间糟糕得多，所以它压过横排。
 * 2. [LoginArrangement.SideBySide] —— Expanded 及以上（≥ 840dp）。用左右空间换掉
 *    "表单居中 + 两侧各留几百 dp"。
 * 3. [LoginArrangement.Single] —— Compact / Medium。
 *    **600–839dp 不做两区不是偷懒**：表单 480dp + 栏间 24dp + 品牌区 ≈ 864dp > 839dp，
 *    塞不下；要塞就得把表单压到 400dp 上下，那就成了第二个 measure 值 ——
 *    多一个需要维护的数字，换不来实际收益。
 *
 * 写成纯函数是为了能用真实 dp 尺寸做守卫测试（见 commonTest），不必真机旋转。
 */
internal fun loginArrangementFor(directive: PaneScaffoldDirective): LoginArrangement = when {
  directive.canShowStackedPanes -> LoginArrangement.Stacked
  directive.canShowSideBySidePanes -> LoginArrangement.SideBySide
  else -> LoginArrangement.Single
}

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

  val directive = LocalPaneScaffoldDirective.current
  val arrangement = remember(directive) { loginArrangementFor(directive) }

  Scaffold { padding ->
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(state = rememberScrollState())
        // 顶部系统内边距加在最外层：品牌区与表单区都在它之下，两边不必各算一次
        .padding(
          top = padding.calculateTopPadding(),
          bottom = padding.calculateBottomPadding(),
        )
        // 底部还要留给键盘，不要把输入框压在它下面。imePadding 必须在 verticalScroll
        // **之内**（即链尾）：它给滚动内容补上键盘高度的底部留白，于是滚动区域的内容变高，
        // 聚焦的输入框能被自动滚进键盘上方的可视区。
        .imePadding()
    ) {
      // 表单区的宽度：三种排布下都**不超过** ContentWidth.Form，差别只在由谁提供剩余空间。
      val formModifier = when (arrangement) {
        // 横排：取精确 measure —— Row 里的剩余空间已经由 weight 交给品牌区，
        // 表单这边再 fillMaxWidth 会把整行吃掉。两区只在 ≥840dp 出现，480dp 必定放得下。
        LoginArrangement.SideBySide -> Modifier.width(ContentWidth.Form)
        // 单栏 / 竖排：窄窗口填满、宽窗口封顶（父级 CenterHorizontally 负责居中）
        else -> Modifier.contentWidth(ContentWidth.Form)
      }

      when (arrangement) {
        LoginArrangement.Single -> FormRegion(
          modifier = formModifier,
          showLogo = true,
          state = state,
          onIntent = viewModel::sendIntent,
          onLoginClick = loginClick,
        )

        LoginArrangement.SideBySide -> Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxWidth(),
        ) {
          BrandPane(modifier = Modifier.weight(1f))
          // 栏间留白用 directive 的值：与列表-详情用的是同一套间隔，全应用视觉一致
          Spacer(modifier = Modifier.width(directive.horizontalPartitionSpacerSize))
          FormRegion(
            modifier = formModifier,
            showLogo = false,
            state = state,
            onIntent = viewModel::sendIntent,
            onLoginClick = loginClick,
          )
        }

        LoginArrangement.Stacked -> {
          BrandPane(modifier = Modifier.fillMaxWidth())
          Spacer(modifier = Modifier.height(directive.verticalPartitionSpacerSize))
          FormRegion(
            modifier = formModifier,
            showLogo = false,
            state = state,
            onIntent = viewModel::sendIntent,
            onLoginClick = loginClick,
          )
        }
      }
    }
  }

  if (state.isLoading) {
    ContainedLoadingIndicator()
  }
}

/**
 * 表单区：顶部两个全局入口 + （可选的）logo + 账户/密码输入 + 登录按钮。
 *
 * @param showLogo 单栏排布时 logo 留在这里；两区排布时它由 [BrandPane] 承担，避免重复出现。
 */
@Composable
private fun FormRegion(
  modifier: Modifier = Modifier,
  showLogo: Boolean,
  state: LoginState,
  onIntent: (LoginIntent) -> Unit,
  onLoginClick: () -> Unit,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
    // 深色模式 / 语言两个入口属于**表单区**，而不是窗口的角落。
    // 原先它们 `align(End)` 贴的是窗口右缘：窗口越宽离表单越远（841dp 上差约 400dp，
    // 1600dp 上差约 800dp）。归入表单区后，窄窗口下表单与窗口同宽、观感与原先一致；
    // 宽窗口下则始终贴着表单。
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .align(Alignment.End)
        .padding(top = 16.dp, end = 16.dp),
    ) {
      SelectIconButton(
        spec = DarkThemePreference.spec,
        value = state.darkTheme,
        onValueChange = { onIntent(LoginIntent.UpdateDarkTheme(it)) },
      )

      SelectIconButton(
        spec = LanguagePreference.spec,
        value = state.appLanguage,
        onValueChange = { onIntent(LoginIntent.UpdateLanguage(it)) },
      )
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(vertical = 32.dp),
    ) {
      if (showLogo) {
        Image(imageVector = AppLogo, contentDescription = null)
      }

      AccountInput(
        modifier = Modifier.padding(top = 40.dp),
        account = state.account,
        accountError = state.accountError,
        onAccountChange = { onIntent(LoginIntent.UpdateAccount(it)) },
        onClearAccount = { onIntent(LoginIntent.ClearAccount) }
      )

      PasswordInput(
        modifier = Modifier.padding(top = 16.dp),
        password = state.password,
        passwordError = state.passwordError,
        passwordVisible = state.passwordVisible,
        onPasswordChange = { onIntent(LoginIntent.UpdatePassword(it)) },
        onPasswordVisibleChange = { onIntent(LoginIntent.UpdatePasswordVisible(it)) },
        onLogin = onLoginClick,
      )

      Button(
        onClick = onLoginClick,
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

/**
 * 品牌区：logo + 应用名 + 一句说明。
 *
 * 只在两区排布（横排 / 竖排）出现 —— 它承接的是"表单封顶之后剩下的空间"，
 * 而不是把 logo 再画一遍。
 *
 * 文案是**占位**：`login_brand_name` 与 Android 侧的 `app_name` 取同一值；
 * `login_brand_tagline` 是随手拟的一句，有正式文案时替换这两个字符串即可。
 */
@Composable
private fun BrandPane(modifier: Modifier = Modifier) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier.padding(vertical = 32.dp, horizontal = 24.dp),
  ) {
    Image(imageVector = AppLogo, contentDescription = null)

    Text(
      text = stringResource(Res.string.login_brand_name),
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.padding(top = 24.dp),
    )

    Text(
      text = stringResource(Res.string.login_brand_tagline),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      // 一句话也是"一行文本"，同样受可读上限约束：品牌区可以很宽，句子不该跟着变很宽
      modifier = Modifier
        .padding(top = 8.dp)
        .width(ContentWidth.Form),
    )
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
