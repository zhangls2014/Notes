package me.zhangls.notes

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.material3.dynamicColorScheme
import me.zhangls.entry.App

class MainActivity : AppCompatActivity() {
  private var deepLinkUrl by mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    deepLinkUrl = intent?.dataString

    setContent {
      val wallpaperColor by rememberWallpaperColor()

      App(
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
          wallpaperColor?.let { seed ->
            dynamicColorScheme(seedColor = seed, isDark = darkTheme)
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
