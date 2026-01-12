package me.zhangls.notes

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import me.zhangls.notes.navigation.AppNavHost
import me.zhangls.theme.ComposeAppTheme

/**
 * @author zhangls
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }

    super.onCreate(savedInstanceState)

    setContent {
      ComposeAppTheme(darkTheme = false, dynamicColor = false) {
        AppNavHost()
      }
    }
  }
}