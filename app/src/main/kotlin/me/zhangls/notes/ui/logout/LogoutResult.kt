package me.zhangls.notes.ui.logout

import me.zhangls.framework.mvi.MviEffect

/**
 * @author zhangls
 */
sealed interface LogoutResult : MviEffect {
  data object Cancel : LogoutResult
  data object Logout : LogoutResult
}