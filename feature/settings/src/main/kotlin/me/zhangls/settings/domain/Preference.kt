package me.zhangls.settings.domain

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * @author zhangls
 */
sealed interface Preference<T> {
  val key: String
  val value: T
  val title: Int
  val summary: Int?
  val icon: ImageVector?

  data class Text(
    override val key: String,
    override val value: Unit = Unit,
    override val title: Int,
    override val summary: Int?,
    override val icon: ImageVector?,
  ) : Preference<Unit>

  data class Switch(
    override val key: String,
    override val value: Boolean,
    override val title: Int,
    override val summary: Int?,
    override val icon: ImageVector?,
  ) : Preference<Boolean>

  data class Alert<T>(
    override val key: String,
    override val value: T,
    override val title: Int,
    override val summary: Int?,
    val options: List<Option<T>>,
    override val icon: ImageVector?,
  ) : Preference<T>

  /**
   * 辅助类：定义每个选项的显示文字和实际值
   */
  data class Option<T>(
    val label: Int,
    val value: T
  )
}