package me.zhangls.email.waterfall

import kotlinx.serialization.Serializable
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
) : MviState
