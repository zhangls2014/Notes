package me.zhangls.framework.nav

import me.zhangls.framework.mvi.MviEffect

/**
 * 页面导航操作定义
 *
 * @author zhangls
 */
sealed interface NavEffect : MviEffect {
  val dest: Destination?

  // 向返回堆栈压入一个新的页面
  data class Navigate(override val dest: Destination) : NavEffect

  // 替换返回堆栈的栈顶页面
  data class Replace(override val dest: Destination) : NavEffect

  // 清空返回堆栈，并压入一个新的页面
  data class Restart(override val dest: Destination) : NavEffect

  // 移除返回堆栈的栈顶页面
  data class Popup(override val dest: Destination? = null) : NavEffect
}