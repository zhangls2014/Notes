package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.zhangls.about.api.AboutEntry
import me.zhangls.entry.data.sample.LocalEmailsDataProvider
import me.zhangls.framework.AppInfo
import me.zhangls.framework.toast.SystemToast
import me.zhangls.login.api.LocalAccountRegistration
import me.zhangls.login.api.LocalPasskeyProvider
import me.zhangls.login.api.LocalPasswordlessAuth
import me.zhangls.login.api.PasskeyProvider
import me.zhangls.model.AppLanguage
import me.zhangls.preference.DarkThemePreference
import me.zhangls.profile.api.AccountProfileRefresh
import me.zhangls.settings.api.LocalPasskeyManagementAvailable
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.LocalPaneScaffoldDirective
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo
import me.zhangls.theme.layout.rememberPaneScaffoldDirective
import me.zhangls.theme.layout.rememberWindowAdaptiveInfo
import notes.composeapp.generated.resources.Res
import notes.composeapp.generated.resources.auth_retry_local_data
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal expect fun rememberSystemToast(): SystemToast

@Composable
fun App(
  onDarkThemeChange: (Boolean) -> Unit = {},
  onLanguageChange: (AppLanguage) -> Unit = {},
  onDynamicColorChange: (darkTheme: Boolean) -> ColorScheme? = { null },
  deepLinkUrl: String? = null,
  onDeepLinkConsume: () -> Unit = {},
  deviceAuthenticator: DeviceAuthenticator = UnavailableDeviceAuthenticator,
  passkeyProvider: PasskeyProvider? = null,
) {
  val viewModel: AppViewModel = koinViewModel()
  val aboutEntry: AboutEntry = koinInject()
  val appInfo: AppInfo = koinInject()
  val version = remember(appInfo) { appInfo.getVersionName() }
  val systemToast = rememberSystemToast()
  val browserUriHandler = rememberBrowserUriHandler()
  val state by viewModel.state.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  val services: AppSessionServices = koinInject()
  val profile: AccountProfileRefresh = koinInject()
  val accounts = services.accounts
  val auth = services.auth
  val enabledPasskeyProvider = passkeyProvider?.takeIf { services.passkeysEnabled && it.isSupported }
  val accountId = state.accountId
  var readyAccountId by remember { mutableStateOf<String?>(null) }
  var accountDataError by remember(accountId) { mutableStateOf(false) }
  var retryAccountData by remember(accountId) { mutableStateOf(0) }
  val accessibleAccount = accountId?.takeIf { state.access == AppAccess.Ready }
  LaunchedEffect(accounts, accessibleAccount, retryAccountData) {
    readyAccountId = null
    accountDataError = false
    try {
      if (accessibleAccount == null) {
        accounts.close()
      } else {
        accounts.open(accessibleAccount, LocalEmailsDataProvider.allEmails)
        readyAccountId = accessibleAccount
      }
    } catch (cancelled: CancellationException) {
      throw cancelled
    } catch (_: Exception) {
      accountDataError = true
    }
  }
  LaunchedEffect(profile, accessibleAccount) {
    if (auth != null && accessibleAccount != null) profile.refresh()
  }
  LaunchedEffect(auth, accessibleAccount) {
    if (auth == null || accessibleAccount == null) return@LaunchedEffect
    while (true) {
      try {
        auth.refresh(accessibleAccount)
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        // The existing local session stays available during transient network failures.
      }
      delay(5 * 60 * 1000L)
    }
  }
  var managingPasskeys by remember(accountId) { mutableStateOf(false) }
  // 各 feature 对导航的贡献由它们自己的 Koin 模块以多绑定登记，这里一处收集
  val navigationRegistry = rememberNavigationRegistry()
  // remember 住解析结果：parseDeepLink 每次调用都会新建对象，不记住的话这个参数会随每次
  // 重组变化。下游需要按**引用**区分"首帧带进来的 DeepLink"与"运行期新来的同一条
  // DeepLink"，参数不稳定会让这个判断失准（等于把每次重组都当成一次新导航）。
  val deepLinkDestination = remember(deepLinkUrl, navigationRegistry) {
    parseDeepLink(deepLinkUrl, navigationRegistry.deepLinkMatchers)
  }
  val currentDeepLink = deepLinkDestination.takeIf {
    auth == null || accessibleAccount != null && isDeepLinkForAccount(deepLinkUrl, accessibleAccount)
  }
  LaunchedEffect(auth, accessibleAccount, readyAccountId, deepLinkUrl) {
    if (auth != null && accessibleAccount != null && readyAccountId == accessibleAccount &&
      deepLinkDestination != null && currentDeepLink == null
    ) onDeepLinkConsume()
  }
  val darkTheme = DarkThemePreference.isDark(state.darkTheme)
  val fontScale = state.fontSize.value

  SideEffect(state.appLanguage) {
    state.appLanguage?.also {
      onLanguageChange(it)
    }
  }

  SideEffect(darkTheme) {
    onDarkThemeChange(darkTheme)
  }

  // 窗口形态在这里算一次，向下只下发事实。放在 ComposeAppTheme **之上**：尺寸类是用
  // LocalDensity 把容器像素换算成 dp 得到的，而主题会覆写 LocalDensity（字号设置）——
  // 让"窗口多大"依赖"字号设置"是隐性错误耦合。
  val windowAdaptiveInfo = rememberWindowAdaptiveInfo()
  // 窗格排布指令是窗口形态的派生结果，同样在这里算一次：需要它的地方不止一处
  // （列表-详情场景策略、登录页的两区排布），各自算一次就会有多个派生点，
  // 参数一旦被谁改掉就会静默分叉。
  val paneScaffoldDirective = rememberPaneScaffoldDirective(windowAdaptiveInfo)

  CompositionLocalProvider(
    LocalUriHandler provides browserUriHandler,
    LocalPasswordlessAuth provides auth,
    LocalAccountRegistration provides auth,
    LocalPasskeyProvider provides enabledPasskeyProvider,
    LocalPasskeyManagementAvailable provides (auth != null),
    LocalWindowAdaptiveInfo provides windowAdaptiveInfo,
    LocalPaneScaffoldDirective provides paneScaffoldDirective,
  ) {
    ComposeAppTheme(
      darkTheme = darkTheme,
      dynamicScheme = if (state.dynamicColor) onDynamicColorChange(darkTheme) else null,
      fontScale = fontScale,
    ) {
      when (state.access) {
        AppAccess.Loading -> Unit
        AppAccess.Locked -> {
          var missingCredential by remember(deviceAuthenticator) {
            mutableStateOf(deviceAuthenticator.isDeviceCredentialMissing())
          }
          UnlockScreen(verifying = state.verifyingDevice, missingCredential = missingCredential) {
            missingCredential = deviceAuthenticator.isDeviceCredentialMissing()
            if (!missingCredential) scope.launch { viewModel.verifyDevice(deviceAuthenticator) }
          }
        }
        AppAccess.SignedOut, AppAccess.Ready -> when {
          accessibleAccount == null || readyAccountId == accessibleAccount -> key(accountId) {
            AppNavHost(
              viewModel = viewModel,
              deepLinkDestination = currentDeepLink.takeIf { state.access == AppAccess.Ready },
              onDeepLinkConsume = onDeepLinkConsume,
              deviceAuthenticator = deviceAuthenticator,
              onManagePasskeys = { managingPasskeys = true },
              accountNavigationManagedByApp = true,
            )
          }
          accountDataError -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextButton(onClick = { retryAccountData++ }) {
              Text(stringResource(Res.string.auth_retry_local_data))
            }
          }
        }
      }

      if (managingPasskeys && auth != null && state.access == AppAccess.Ready) {
        PasskeyManagementDialog(
          listPasskeys = auth::listPasskeys,
          addPasskey = if (enabledPasskeyProvider != null) {
            { auth.addPasskey(enabledPasskeyProvider) }
          } else {
            null
          },
          revokePasskey = auth::revokePasskey,
          onDismiss = { managingPasskeys = false },
        )
      }

      if ((state.access == AppAccess.SignedOut || state.access == AppAccess.Ready) &&
        state.settingsLoaded && state.lastReadReleaseVersion != version
      ) {
        aboutEntry.ReleaseNotesDialog(
          version = version,
          onDismiss = { viewModel.sendIntent(AppIntent.DismissReleaseNotes(version)) },
        )
      }

      LaunchedEffect(viewModel, systemToast) {
        viewModel.toast.collect { effect ->
          val message = getString(effect.resId)
          systemToast.show(message, longDuration = message.length > 20)
        }
      }
    }
  }
}
