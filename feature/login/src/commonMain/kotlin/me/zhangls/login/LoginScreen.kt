@file:OptIn(ExperimentalMaterial3AdaptiveApi::class)

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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AdaptStrategy
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
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
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo
import me.zhangls.theme.layout.contentWidth
import me.zhangls.theme.layout.isTabletopPosture
import notes.feature.login.generated.resources.Res
import notes.feature.login.generated.resources.login_action_login
import notes.feature.login.generated.resources.login_brand_name
import notes.feature.login.generated.resources.login_brand_tagline
import notes.feature.login.generated.resources.login_hint_login_account
import notes.feature.login.generated.resources.login_hint_login_password
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * 登录页的窗格方案：交给库的窗格脚手架之后，本页只剩"用哪份指令 + 得到什么窗格取值"。
 *
 * 为什么不再自己排布（原先是一个 `loginArrangementFor` 纯函数 + 自绘 Row/Column）：
 * 自绘排布**看不到铰链**。2026-09-22 的实测里，书本式折叠设备半开（竖向铰链落在 `x=884`）时，
 * 居中于窗口的表单其输入框横跨 `x=[338,1430]` —— 铰链线正好穿过输入框中心；
 * 而列表-详情那边因为走库的窗格装配，内容被自动限制在铰链一侧。同一个窗口两种待遇，
 * 差别只在"有没有消费 directive"。
 *
 * 现在换成 [SupportingPaneScaffold]：主窗格 = 表单，支持窗格 = 品牌。
 * 竖向铰链由 `directive.excludedBounds` 交给 `ThreePaneScaffold` 切分区，内容自然不跨铰链；
 * 横向铰链（桌面支架）则见 [loginPanePlan]。
 */
internal class LoginPanePlan(
  /** 实际交给脚手架的指令 —— 桌面支架下与 [PaneScaffoldDirective] 传来的那份不同，见 [loginPanePlan]。 */
  val directive: PaneScaffoldDirective,
  val value: ThreePaneScaffoldValue,
) {
  private val supportingValue: PaneAdaptedValue
    get() = value[SupportingPaneScaffoldRole.Supporting]

  /**
   * 品牌窗格此刻是否真的显示。
   *
   * 窄窗口上它不显示，此时 logo 留在表单列顶部（否则页面上会一个图标都没有）；
   * 两区排布时 logo 归品牌区，表单列里不再重复画一遍。
   */
  val isBrandPaneShown: Boolean
    get() = supportingValue != PaneAdaptedValue.Hidden

  /** 品牌窗格是否被**叠到表单之下**（桌面支架：窗口被横向铰链切成两半）。 */
  val isBrandPaneReflowed: Boolean
    get() = supportingValue is PaneAdaptedValue.Reflowed

  /** 表单窗格当前是否与品牌窗格**并排**（而非独占整幅宽度）。 */
  val isFormPaneSideBySide: Boolean
    get() = supportingValue == PaneAdaptedValue.Expanded
}

/**
 * 由窗口形态与窗格指令算出登录页的窗格方案。**纯函数**，因此能用真实 dp 尺寸做守卫测试
 * （见 commonTest），不必真机旋转、也不必找一台折叠设备。
 *
 * 两处决策都属"本页的意图声明"，而不是重新实现窗格算法：
 *
 * 1. **桌面支架（横向铰链）下把横向分区压到 1**。库的窗格排版只切**竖向**铰链
 *    （`ThreePaneScaffold` 用的 `directive.excludedBounds` 由 `getExcludedVerticalBounds`
 *    算出，只有竖向）；横向铰链靠 `maxVerticalPartitions` + `AdaptStrategy.Reflow`
 *    把窗格**上下叠放**来避让。而 reflow 的前提是 `maxHorizontalPartitions == 1`
 *    （见 `calculateThreePaneScaffoldValue` 的 `checkReflowedPane`）——
 *    不压这一档，宽窗口上的桌面支架会拿到两栏并排，**两栏各自横跨铰链**。
 * 2. **品牌窗格的两种不显示方式**：
 *    - 空间不够（只有 1 个横向分区，且没有横向铰链）→ [AdaptStrategy.Hide]，整块不显示；
 *    - 桌面支架 → [AdaptStrategy.Reflow]，叠到表单之下（同一个横向分区里的第二个竖向分区）。
 *
 *    ⚠️ 判据是**姿态**，不是"竖向有空间"。directive 的 `maxVerticalPartitions == 2` 在
 *    "窄而高"的普通竖屏手机上也成立（411×914dp 实测），按它选 reflow 会把手机摊成
 *    上下半屏：品牌占掉上半屏、两个字段落到下半屏、中间空出一大段。手机上没有铰链要避。
 */
internal fun loginPanePlan(
  adaptiveInfo: WindowAdaptiveInfo,
  directive: PaneScaffoldDirective,
): LoginPanePlan {
  val isTabletop = adaptiveInfo.isTabletopPosture

  // 桌面支架：压到单横向分区，让 reflow 生效（理由见上文第 1 条）
  val effectiveDirective = if (isTabletop) {
    directive.copy(maxHorizontalPartitions = 1)
  } else {
    directive
  }

  val adaptStrategies = SupportingPaneScaffoldDefaults.adaptStrategies(
    supportingPaneAdaptStrategy = if (isTabletop) {
      AdaptStrategy.Reflow(SupportingPaneScaffoldRole.Main)
    } else {
      AdaptStrategy.Hide
    },
  )

  // 本页没有窗格间的导航（不支持在"品牌 / 表单"之间跳转），所以当前目的地恒为 null：
  // 优先级回落到 Primary → Secondary → Tertiary，正是"表单优先、品牌其次"。
  val value = calculateThreePaneScaffoldValue(
    maxHorizontalPartitions = effectiveDirective.maxHorizontalPartitions,
    adaptStrategies = adaptStrategies,
    currentDestination = null,
    maxVerticalPartitions = effectiveDirective.maxVerticalPartitions,
  )

  return LoginPanePlan(effectiveDirective, value)
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

  val plan = rememberLoginPanePlan()

  Scaffold { padding ->
    SupportingPaneScaffold(
      directive = plan.directive,
      value = plan.value,
      // 系统栏内边距加在脚手架外层：两个窗格都在安全区内，各自不必再算一次。
      // 键盘内边距则要加在**表单窗格内部**（见 [FormPane]）—— 桌面支架下键盘占的是下半屏，
      // 也就是品牌窗格那一侧，套在外层会让表单跟着缩，反而把内容挤走。
      modifier = Modifier.padding(padding),
      mainPane = {
        AnimatedPane {
          FormPane(
            showLogo = !plan.isBrandPaneShown,
            state = state,
            onIntent = viewModel::sendIntent,
            onLoginClick = loginClick,
          )
        }
      },
      supportingPane = {
        AnimatedPane {
          // 被叠到表单之下时竖向空间至多一半，省掉那句说明，避免在小窗格里被裁
          BrandPane(showTagline = !plan.isBrandPaneReflowed)
        }
      },
    )
  }

  if (state.isLoading) {
    ContainedLoadingIndicator()
  }
}

@Composable
private fun rememberLoginPanePlan(): LoginPanePlan {
  val adaptiveInfo = LocalWindowAdaptiveInfo.current
  val directive = LocalPaneScaffoldDirective.current
  return remember(adaptiveInfo, directive) { loginPanePlan(adaptiveInfo, directive) }
}

/**
 * 表单窗格（主窗格）：顶部两个全局入口 + （窄窗口下的）logo + 账户/密码输入 + 登录按钮。
 *
 * @param showLogo 品牌窗格不显示时（窄窗口）logo 留在这里，否则由 [BrandPane] 承担，避免重复出现。
 */
@Composable
private fun FormPane(
  showLogo: Boolean,
  state: LoginState,
  onIntent: (LoginIntent) -> Unit,
  onLoginClick: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(state = rememberScrollState())
      // imePadding 必须在 verticalScroll **之内**（即链尾）：它给滚动内容补上键盘高度的底部
      // 留白，于是滚动区域的内容变高，聚焦的输入框能被自动滚进键盘上方的可视区。
      .imePadding(),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      // 窗格可以宽到 800dp 以上（1600dp 窗口下的并排），表单仍受可读上限约束、并在窗格内居中
      modifier = Modifier.contentWidth(ContentWidth.Form),
    ) {
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
}

/**
 * 品牌窗格（支持窗格）：logo + 应用名 + 一句说明。
 *
 * 只在空间够放两区时出现 —— 它承接的是"表单封顶之后剩下的空间"，而不是把 logo 再画一遍。
 *
 * 文案是**占位**：`login_brand_name` 与 Android 侧的 `app_name` 取同一值；
 * `login_brand_tagline` 是随手拟的一句，有正式文案时替换这两个字符串即可。
 *
 * @param showTagline 叠到表单之下时竖向空间减半，此时省略那句说明。
 */
@Composable
private fun BrandPane(showTagline: Boolean, modifier: Modifier = Modifier) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
      .fillMaxSize()
      .padding(vertical = 32.dp, horizontal = 24.dp),
  ) {
    Image(imageVector = AppLogo, contentDescription = null)

    Text(
      text = stringResource(Res.string.login_brand_name),
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.padding(top = 24.dp),
    )

    if (showTagline) {
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
