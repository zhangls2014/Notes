package me.zhangls.main

import com.mohamedrejeb.calf.io.KmpFile
import me.zhangls.framework.mvi.MviIntent
import org.jetbrains.compose.resources.StringResource

/**
 * @author zhangls
 */
sealed class EmailIntent : MviIntent {
  data class ShowToast(val res: StringResource) : EmailIntent()
  data class UpdateFavorite(val emailId: Long) : EmailIntent()
  data class UpdateSelectedEmail(val emailId: Long) : EmailIntent()
  data class UpdateSearchText(val text: CharSequence) : EmailIntent()
  data class UpdateSelectedAvatar(val avatar: KmpFile?) : EmailIntent()
  data object MultiFavorite : EmailIntent()
  data object MultiCancelFavorite : EmailIntent()
  data object MultiDelete : EmailIntent()
  data object ClearSelectedEmail : EmailIntent()
}