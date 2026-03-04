package me.zhangls.notes.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import me.zhangls.data.type.DarkThemeConfig
import me.zhangls.notes.parseDeepLink
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.component.ToastHost
import me.zhangls.theme.component.rememberToastState

/**
 * @author zhangls
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    // SplashScreen
    installSplashScreen()
    // EdgeToEdge
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }

    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)

    val uri = intent.data
    val destination = parseDeepLink(uri)

    setContent {
      val viewmodel: MainViewModel = hiltViewModel()
      val toastState = rememberToastState()
      val state by viewmodel.state.collectAsState()
      val darkTheme = when (state.darkTheme) {
        DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        DarkThemeConfig.LIGHT -> false
        DarkThemeConfig.DARK -> true
      }

      ComposeAppTheme(darkTheme = darkTheme, dynamicColor = state.dynamicColor) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          AppNavHost(viewmodel = viewmodel, deepLinkDestination = destination)
          ToastHost(toastState)
        }

        LaunchedEffect(viewmodel.toast) {
          viewmodel.toast.collect {
            toastState.showToast(it.resId)
          }
        }
      }
    }
  }
}