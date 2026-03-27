package me.zhangls.notes

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import me.zhangls.entry.ui.App


class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    // TODO DeepLink 解析
    intent.data

    setContent {
      val context = LocalContext.current

      App(
        onDarkThemeChanged = {
          val lightBars = it.not()
          val controller = WindowInsetsControllerCompat(window, window.decorView)
          // 状态栏图标颜色（亮色图标 = 深色背景）
          controller.isAppearanceLightStatusBars = lightBars
          // 导航栏图标颜色
          controller.isAppearanceLightNavigationBars = lightBars
        },
        onLanguageChanged = {
          AppCompatDelegate.setApplicationLocales(it.toLocales())
        },
        onDynamicColorChanged = { darkTheme ->
          (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
          } else {
            null
          })
        }
      )
    }
  }
}