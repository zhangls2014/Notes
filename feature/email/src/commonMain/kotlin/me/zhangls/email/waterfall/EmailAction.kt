package me.zhangls.email.waterfall

import me.zhangls.data.model.UserModel
import me.zhangls.framework.mvi.MviAction

/**
 * @author zhangls
 */
sealed interface EmailAction : MviAction {
  data object ClearSelectedEmail : EmailAction
  data class UpdateUser(val user: UserModel?) : EmailAction
  data class UpdateSelectedEmail(val emailId: Long) : EmailAction
  data class UpdateSearchText(val text: String) : EmailAction
}
