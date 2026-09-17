package me.zhangls.settings.ui

import androidx.compose.ui.graphics.vector.ImageVector
import me.zhangls.settings.mvi.SettingsIntent
import me.zhangls.theme.ThemeColor
import org.jetbrains.compose.resources.StringResource

/**
 * 设置项的表现层模型：持有图标、文案资源等 UI 数据，
 * 并通过 [onValueChange] / [clickIntent] 携带对应的 [SettingsIntent]，
 * 供 Compose 在用户交互时直接派发。
 *
 * @author zhangls
 */
sealed interface PreferenceUiModel {
  val key: String
  val title: StringResource
  val summary: StringResource?
  val icon: ImageVector?

  data class Text(
    override val key: String,
    override val title: StringResource,
    override val summary: StringResource?,
    override val icon: ImageVector?,
    val tint: ThemeColor? = null,
    val clickIntent: SettingsIntent,
  ) : PreferenceUiModel

  data class Switch(
    override val key: String,
    val value: Boolean,
    override val title: StringResource,
    override val summary: StringResource?,
    override val icon: ImageVector?,
    val onValueChange: (Boolean) -> SettingsIntent,
  ) : PreferenceUiModel

  data class Alert<T>(
    override val key: String,
    val value: T,
    override val title: StringResource,
    override val summary: StringResource?,
    val options: List<Option<T>>,
    override val icon: ImageVector?,
    val onValueChange: (T) -> SettingsIntent,
  ) : PreferenceUiModel

  /**
   * 辅助类：定义每个选项的显示文字和实际值
   */
  data class Option<T>(
    val label: StringResource,
    val value: T
  )
}
