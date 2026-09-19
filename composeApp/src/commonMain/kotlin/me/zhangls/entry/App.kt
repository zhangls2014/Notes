package me.zhangls.entry

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.framework.toast.showSystemToast
import me.zhangls.model.AppLanguage
import me.zhangls.data.type.DarkThemeConfig
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
  val deepLinkDestination = parseDeepLink(deepLinkUrl)
  val darkTheme = when (state.darkTheme) {
    DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    DarkThemeConfig.LIGHT -> false
    DarkThemeConfig.DARK -> true
  }
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
