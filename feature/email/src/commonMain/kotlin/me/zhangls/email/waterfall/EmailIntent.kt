package me.zhangls.email.waterfall

import me.zhangls.framework.mvi.MviIntent
import org.jetbrains.compose.resources.StringResource

/**
 * @author zhangls
 */
sealed class EmailIntent : MviIntent {
  data class ShowToast(val res: StringResource) : EmailIntent()
  data class UpdateFavorite(val emailId: Long) : EmailIntent()
  data class UpdateSelectedEmail(val emailId: Long) : EmailIntent()
  data object MultiFavorite : EmailIntent()
  data object MultiCancelFavorite : EmailIntent()
  data object MultiDelete : EmailIntent()
  data object ClearSelectedEmail : EmailIntent()
}
