package me.zhangls.main

import me.zhangls.framework.mvi.MviIntent

/**
 * @author zhangls
 */
sealed class EmailIntent : MviIntent {
  data class ShowToast(val resId: Int) : EmailIntent()
  data class UpdateFavorite(val emailId: Long) : EmailIntent()
  data class UpdateSelectedEmail(val emailId: Long) : EmailIntent()
  data class UpdateSearchText(val text: CharSequence) : EmailIntent()
  data object MultiFavorite : EmailIntent()
  data object MultiCancelFavorite : EmailIntent()
  data object MultiDelete : EmailIntent()
  data object ClearSelectedEmail : EmailIntent()
}