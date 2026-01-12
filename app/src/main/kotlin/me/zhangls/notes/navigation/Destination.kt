package me.zhangls.notes.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 返回堆栈的键，每个页面都需要定义一个对应的键。
 *
 * @author zhangls
 */
@Serializable
sealed class Destination(
  /**
   * 是否需要登录
   */
  val requiresLogin: Boolean
) : NavKey {
  @Serializable
  data object Login : Destination(requiresLogin = false)

  @Serializable
  data object Home : Destination(requiresLogin = true)

  @Serializable
  data class Detail(val id: Int) : Destination(requiresLogin = true)
}