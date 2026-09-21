package me.zhangls.entry

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.framework.toast.showSystemToast
import me.zhangls.model.AppLanguage
import me.zhangls.preference.DarkThemePreference
import me.zhangls.theme.ComposeAppTheme
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun App(
  onDarkThemeChanged: (Boolean) -> Unit = {},
  onLanguageChanged: (AppLanguage) -> Unit = {},
  onDynamicColorChanged: (darkTheme: Boolean) -> ColorScheme? = { null },
  deepLinkUrl: String? = null,
  onDeepLinkConsumed: () -> Unit = {},
) {
  val viewModel: AppViewModel = koinViewModel()
  val state by viewModel.state.collectAsStateWithLifecycle()
  // remember 住解析结果：parseDeepLink 每次调用都会新建对象，不记住的话这个参数会随每次
  // 重组变化。下游需要按**引用**区分"首帧带进来的 DeepLink"与"运行期新来的同一条
  // DeepLink"，参数不稳定会让这个判断失准（等于把每次重组都当成一次新导航）。
  val deepLinkDestination = remember(deepLinkUrl) { parseDeepLink(deepLinkUrl) }
  val darkTheme = DarkThemePreference.isDark(state.darkTheme)
  val fontScale = state.fontSize.value

  LaunchedEffect(state.appLanguage) {
    state.appLanguage?.also {
      onLanguageChanged(it)
    }
  }

  LaunchedEffect(darkTheme) {
    onDarkThemeChanged(darkTheme)
  }

  ComposeAppTheme(
    darkTheme = darkTheme,
    dynamicScheme = if (state.dynamicColor) onDynamicColorChanged(darkTheme) else null,
    fontScale = fontScale,
  ) {
    AppNavHost(
      viewModel = viewModel,
      deepLinkDestination = deepLinkDestination,
      onDeepLinkConsumed = onDeepLinkConsumed
    )

    LaunchedEffect(Unit) {
      viewModel.toast.collect { effect ->
        val message = getString(effect.resId)
        showSystemToast(message, longDuration = message.length > 20)
      }
    }
  }
}
