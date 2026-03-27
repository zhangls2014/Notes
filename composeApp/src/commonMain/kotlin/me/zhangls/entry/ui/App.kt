package me.zhangls.entry.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.data.type.AppLanguage
import me.zhangls.data.type.DarkThemeConfig
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.component.ToastHost
import me.zhangls.theme.component.rememberToastState
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun App(
  onDarkThemeChanged: (Boolean) -> Unit = {},
  onLanguageChanged: (AppLanguage) -> Unit = {},
  onDynamicColorChanged: (darkTheme: Boolean) -> ColorScheme? = { null },
) {
  val viewmodel: MainViewModel = koinViewModel()
  val toastState = rememberToastState()
  val state by viewmodel.state.collectAsStateWithLifecycle()
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      AppNavHost(viewmodel = viewmodel, deepLinkDestination = null)
      ToastHost(toastState)
    }

    LaunchedEffect(Unit) {
      viewmodel.toast.collect {
        toastState.showToast(it.resId)
      }
    }
  }
}