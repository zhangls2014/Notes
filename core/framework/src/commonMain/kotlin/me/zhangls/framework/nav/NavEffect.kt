package me.zhangls.framework.nav

import me.zhangls.framework.mvi.MviEffect

/**
 * 页面导航操作定义
 *
 * @author zhangls
 */
sealed interface NavEffect : MviEffect {
  // 向返回堆栈压入一个新的页面
  data class Navigate(val dest: Destination) : NavEffect

  // 替换返回堆栈的栈顶页面
  data class Replace(val dest: Destination) : NavEffect

  /**
   * 以该目的地重建返回堆栈：清空后压入。
   *
   * 用于退出登录等需要重置导航历史的流程。顶层 Tab 选择由组合根单独处理。
   */
  data class Restart(val dest: Destination) : NavEffect

  // 移除返回堆栈的栈顶页面
  data object Popup : NavEffect
}
