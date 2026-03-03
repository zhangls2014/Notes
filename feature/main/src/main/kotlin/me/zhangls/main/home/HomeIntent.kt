package me.zhangls.main.home

import me.zhangls.framework.mvi.MviIntent

/**
 * @author zhangls
 */
sealed class HomeIntent : MviIntent {
  data class ShowToast(val resId: Int) : HomeIntent()
}