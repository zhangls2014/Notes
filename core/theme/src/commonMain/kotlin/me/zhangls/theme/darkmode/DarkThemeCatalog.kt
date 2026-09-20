package me.zhangls.theme.darkmode

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import me.zhangls.model.DarkThemeConfig
import me.zhangls.theme.icon.DarkMode
import me.zhangls.theme.icon.Icons
import notes.core.theme.generated.resources.Res
import notes.core.theme.generated.resources.theme_dark_mode_dark
import notes.core.theme.generated.resources.theme_dark_mode_follow_system
import notes.core.theme.generated.resources.theme_dark_mode_light
import notes.core.theme.generated.resources.theme_label_dark_mode
import org.jetbrains.compose.resources.StringResource

/**
 * 单个深色主题选项：展示文案与实际取值。
 *
 * @author zhangls
 */
data class DarkThemeOption(
  val label: StringResource,
  val value: DarkThemeConfig,
)

/**
 * 深色主题选项的共享目录。
 *
 * 深色模式切换在登录页与设置页均需展示，其标题 / 选项 / 图标属于跨 feature 的共享 UI 数据，
 * 因此与 [me.zhangls.theme.language.LanguageCatalog] 一样下沉到 `core:theme`，
 * 供各方复用，避免 feature 间为此产生直接依赖，也避免两处各维护一份文案。
 *
 * @author zhangls
 */
object DarkThemeCatalog {
  val title: StringResource = Res.string.theme_label_dark_mode

  val icon: ImageVector = Icons.Rounded.DarkMode

  val options: List<DarkThemeOption> = listOf(
    DarkThemeOption(Res.string.theme_dark_mode_follow_system, DarkThemeConfig.FOLLOW_SYSTEM),
    DarkThemeOption(Res.string.theme_dark_mode_light, DarkThemeConfig.LIGHT),
    DarkThemeOption(Res.string.theme_dark_mode_dark, DarkThemeConfig.DARK),
  )

  /**
   * 选项的完整说明文案（设置页摘要用）。
   */
  fun summary(darkTheme: DarkThemeConfig): StringResource = when (darkTheme) {
    DarkThemeConfig.FOLLOW_SYSTEM -> Res.string.theme_dark_mode_follow_system
    DarkThemeConfig.LIGHT -> Res.string.theme_dark_mode_light
    DarkThemeConfig.DARK -> Res.string.theme_dark_mode_dark
  }

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
