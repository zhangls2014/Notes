package me.zhangls.main

import me.zhangls.data.database.entity.AccountEntity
import me.zhangls.framework.mvi.MviAction

/**
 * @author zhangls
 */
sealed interface EmailAction : MviAction {
  data object ClearSelectedEmail : EmailAction
  data class UpdateOwnerAccount(val account: AccountEntity?) : EmailAction
  data class UpdateSelectedEmail(val emailId: Long) : EmailAction
  data class UpdateSearchText(val text: String) : EmailAction
}