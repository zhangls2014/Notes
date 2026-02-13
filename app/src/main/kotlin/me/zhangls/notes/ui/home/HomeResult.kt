package me.zhangls.notes.ui.home

import me.zhangls.framework.mvi.MviEffect

/**
 * @author zhangls
 */
sealed interface HomeResult : MviEffect {
  data class Detail(val id: Int) : HomeResult
  data object Logout : HomeResult
}