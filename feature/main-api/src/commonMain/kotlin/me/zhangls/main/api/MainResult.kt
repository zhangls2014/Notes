package me.zhangls.main.api

import me.zhangls.framework.mvi.MviEffect

/**
 * @author zhangls
 */
sealed interface MainResult : MviEffect {
  data object Logout : MainResult
  data class NavigateToEmailDetail(val emailId: Long) : MainResult
}
