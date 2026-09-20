package me.zhangls.preference

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import me.zhangls.model.DarkThemeConfig
import me.zhangls.theme.icon.DarkMode
import me.zhangls.theme.icon.Icons
import notes.core.preference.generated.resources.Res
import notes.core.preference.generated.resources.theme_dark_mode_dark
import notes.core.preference.generated.resources.theme_dark_mode_follow_system
import notes.core.preference.generated.resources.theme_dark_mode_light
import notes.core.preference.generated.resources.theme_label_dark_mode

/**
 * 深色模式的设置项元数据。
 *
 * 与 [LanguagePreference] 同理：登录页与设置页都要展示同一组选项，故登记在此处。
 *
 * @author zhangls
 */
object DarkThemePreference {
  val spec: PreferenceSpec.Select<DarkThemeConfig> = PreferenceSpec.Select(
    key = "darkTheme",
    title = Res.string.theme_label_dark_mode,
    icon = Icons.Rounded.DarkMode,
    options = listOf(
      PreferenceOption(Res.string.theme_dark_mode_follow_system, DarkThemeConfig.FOLLOW_SYSTEM),
      PreferenceOption(Res.string.theme_dark_mode_light, DarkThemeConfig.LIGHT),
      PreferenceOption(Res.string.theme_dark_mode_dark, DarkThemeConfig.DARK),
    ),
  )

  /**
   * 当前是否应渲染为深色。
   *
   * [DarkThemeConfig.FOLLOW_SYSTEM] 交由系统判定，其余为显式值。
   * 这是"当前是否深色"的唯一判定口径，主题装配与依赖该状态的 UI 都应走这里。
   */
  @Composable
  fun isDark(darkTheme: DarkThemeConfig): Boolean = when (darkTheme) {
    DarkThemeConfig.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    DarkThemeConfig.LIGHT -> false
    DarkThemeConfig.DARK -> true
  }
}
