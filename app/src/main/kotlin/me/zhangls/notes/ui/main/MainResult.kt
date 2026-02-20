package me.zhangls.notes.ui.main

import me.zhangls.framework.mvi.MviEffect

/**
 * @author zhangls
 */
sealed interface MainResult : MviEffect {
  data object Logout : MainResult
}