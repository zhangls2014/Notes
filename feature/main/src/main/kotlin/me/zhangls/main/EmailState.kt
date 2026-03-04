package me.zhangls.main

import kotlinx.serialization.Serializable
import me.zhangls.data.model.AccountModel
import me.zhangls.framework.mvi.MviState

/**
 * @author zhangls
 */
@Serializable
data class EmailState(
  val selectedItems: Set<Long> = emptySet(),
  val ownerAccount: AccountModel? = null,
  val searchText: CharSequence = "",
) : MviState
