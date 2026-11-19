package me.zhangls.notes

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.material3.dynamicColorScheme
import me.zhangls.entry.App

class MainActivity : AppCompatActivity() {
  private val authenticationState: AndroidDeviceAuthenticationState by viewModels()
  private var deepLinkUrl by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    deepLinkUrl = intent?.dataString
    val deviceAuthenticator = AndroidDeviceAuthenticator(this, authenticationState)
    val passkeyProvider = AndroidPasskeyProvider(this)

    setContent {
      val themeColorSource by rememberThemeColorSource()
      val context = LocalContext.current

      App(
        deviceAuthenticator = deviceAuthenticator,
        passkeyProvider = passkeyProvider,
        onDarkThemeChange = {
          val lightBars = it.not()
          WindowInsetsControllerCompat(window, window.decorView).run {
            // 状态栏图标颜色（亮色图标 = 深色背景）
            isAppearanceLightStatusBars = lightBars
            // 导航栏图标颜色
            isAppearanceLightNavigationBars = lightBars
          }
        },
        onLanguageChange = {
          AppCompatDelegate.setApplicationLocales(it.toLocales())
        },
        onDynamicColorChange = { darkTheme ->
          if (themeColorSource.source == "preset" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
          } else {
            themeColorSource.wallpaperColor?.let { seed ->
              dynamicColorScheme(seedColor = seed, isDark = darkTheme)
            }
          }
        },
        deepLinkUrl = deepLinkUrl,
        onDeepLinkConsume = { deepLinkUrl = null },
      )
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    deepLinkUrl = intent.dataString
  }
}
