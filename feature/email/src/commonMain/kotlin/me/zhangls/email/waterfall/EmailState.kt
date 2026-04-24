package me.zhangls.email.waterfall

import kotlinx.serialization.Serializable
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.UserModel
import me.zhangls.framework.mvi.MviState

/**
 * @author zhangls
 */
@Serializable
data class EmailState(
  val selectedItems: Set<Long> = emptySet(),
  val user: UserModel? = null,
  val searchText: CharSequence = "",
  val isEmailDraftVisible: Boolean = false,
  val isSending: Boolean = false,
  val accounts: List<AccountModel> = emptyList(),
  val draftRecipientIds: Set<Long> = emptySet(),
  val draftSubject: String = "",
  val draftBody: String = "",
) : MviState
