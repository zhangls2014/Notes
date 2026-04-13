package me.zhangls.settings.domain

import androidx.compose.ui.graphics.vector.ImageVector
import me.zhangls.theme.ThemeColor
import org.jetbrains.compose.resources.StringResource

/**
 * @author zhangls
 */
sealed interface Preference<T> {
  val key: String
  val value: T
  val title: StringResource
  val summary: StringResource?
  val icon: ImageVector?

  data class Text(
    override val key: String,
    override val value: Unit = Unit,
    override val title: StringResource,
    override val summary: StringResource?,
    override val icon: ImageVector?,
    val tint: ThemeColor? = null,
  ) : Preference<Unit>

  data class Switch(
    override val key: String,
    override val value: Boolean,
    override val title: StringResource,
    override val summary: StringResource?,
    override val icon: ImageVector?,
  ) : Preference<Boolean>

  data class Alert<T>(
    override val key: String,
    override val value: T,
    override val title: StringResource,
    override val summary: StringResource?,
    val options: List<Option<T>>,
    override val icon: ImageVector?,
  ) : Preference<T>

  /**
   * 辅助类：定义每个选项的显示文字和实际值
   */
  data class Option<T>(
    val label: StringResource,
    val value: T
  )
}
