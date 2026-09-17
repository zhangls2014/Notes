package me.zhangls.email.mvi

import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.UserModel
import me.zhangls.framework.mvi.MviAction

/**
 * @author zhangls
 */
sealed interface EmailAction : MviAction {
  data object ClearSelectedEmail : EmailAction
  data class UpdateUser(val user: UserModel?) : EmailAction
  data class UpdateSelectedEmail(val emailId: Long) : EmailAction
  data class SetDraftVisible(val visible: Boolean) : EmailAction
  data class SetSending(val sending: Boolean) : EmailAction
  data class UpdateAllAccounts(val accounts: List<AccountModel>) : EmailAction
  data class UpdateDraftRecipients(val recipientId: Long, val selected: Boolean) : EmailAction
  data class UpdateDraftSubject(val subject: String) : EmailAction
  data class UpdateDraftBody(val body: String) : EmailAction
  data object ClearDraft : EmailAction
}
