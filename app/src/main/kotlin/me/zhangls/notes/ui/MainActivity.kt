package me.zhangls.notes.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.zhangls.data.type.DarkThemeConfig
import me.zhangls.notes.parseDeepLink
import me.zhangls.notes.util.toLocales
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.component.ToastHost
import me.zhangls.theme.component.rememberToastState
import org.koin.compose.viewmodel.koinViewModel


class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }

    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val uri = intent.data
    val destination = parseDeepLink(uri)

    setContent {
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
          AppCompatDelegate.setApplicationLocales(it.toLocales())
        }
      }

      ComposeAppTheme(darkTheme = darkTheme, dynamicColor = state.dynamicColor, fontScale = fontScale) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          AppNavHost(viewmodel = viewmodel, deepLinkDestination = destination)
          ToastHost(toastState)
        }

        LaunchedEffect(Unit) {
          viewmodel.toast.collect {
            toastState.showToast(it.resId)
          }
        }
      }
    }
  }
}
