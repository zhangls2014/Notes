package me.zhangls.entry

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.about.api.AboutEntry
import me.zhangls.framework.AppInfo
import me.zhangls.framework.toast.SystemToast
import me.zhangls.model.AppLanguage
import me.zhangls.preference.DarkThemePreference
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.LocalPaneScaffoldDirective
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo
import me.zhangls.theme.layout.rememberPaneScaffoldDirective
import me.zhangls.theme.layout.rememberWindowAdaptiveInfo
import org.jetbrains.compose.resources.getString
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
) {
  val viewModel: AppViewModel = koinViewModel()
  val aboutEntry: AboutEntry = koinInject()
  val appInfo: AppInfo = koinInject()
  val version = remember(appInfo) { appInfo.getVersionName() }
  val systemToast = rememberSystemToast()
  val browserUriHandler = rememberBrowserUriHandler()
  val state by viewModel.state.collectAsStateWithLifecycle()
  // 各 feature 对导航的贡献由它们自己的 Koin 模块以多绑定登记，这里一处收集
  val navigationRegistry = rememberNavigationRegistry()
  // remember 住解析结果：parseDeepLink 每次调用都会新建对象，不记住的话这个参数会随每次
  // 重组变化。下游需要按**引用**区分"首帧带进来的 DeepLink"与"运行期新来的同一条
  // DeepLink"，参数不稳定会让这个判断失准（等于把每次重组都当成一次新导航）。
  val deepLinkDestination = remember(deepLinkUrl, navigationRegistry) {
    parseDeepLink(deepLinkUrl, navigationRegistry.deepLinkMatchers)
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
    LocalWindowAdaptiveInfo provides windowAdaptiveInfo,
    LocalPaneScaffoldDirective provides paneScaffoldDirective,
  ) {
    ComposeAppTheme(
      darkTheme = darkTheme,
      dynamicScheme = if (state.dynamicColor) onDynamicColorChange(darkTheme) else null,
      fontScale = fontScale,
    ) {
      AppNavHost(
        viewModel = viewModel,
        deepLinkDestination = deepLinkDestination,
        onDeepLinkConsume = onDeepLinkConsume,
      )

      if (state.isLogin != null && state.settingsLoaded && state.lastReadReleaseVersion != version) {
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
