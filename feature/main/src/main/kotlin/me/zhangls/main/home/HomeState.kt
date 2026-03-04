package me.zhangls.main.home

import kotlinx.serialization.Serializable
import me.zhangls.data.model.AccountModel
import me.zhangls.framework.mvi.MviState

/**
 * @author zhangls
 */
@Serializable
data class HomeState(
  val selectedItems: Set<Long> = emptySet(),
  val ownerAccount: AccountModel? = null,
  val searchText: CharSequence = "",
) : MviState
