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
   * 用于"切换到某个容器"这类动作 —— 登出回登录页、点击切换 Tab。被压入的目的地若不是栈底
   * 容器（如非起始 Tab），宿主会先垫上起始 Tab，这样"按返回回起始 Tab"就是返回栈自身的语义，
   * 不需要额外的返回键拦截。
   */
  data class Restart(val dest: Destination) : NavEffect

  // 移除返回堆栈的栈顶页面
  data object Popup : NavEffect
}
