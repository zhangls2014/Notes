package me.zhangls.email.mvi

import kotlinx.serialization.Serializable
import me.zhangls.data.model.AccountModel
import me.zhangls.data.model.UserModel
import me.zhangls.framework.mvi.MviState

/**
 * 含 [UserModel]（accessToken / refreshToken）与账户列表 —— 属敏感数据 + 大对象。
 * 依赖 MviViewModel 的默认非持久化行为，不要为它指定 savedKey；
 * 列表数据可从 Room 重新加载，纯内存态即可。
 *
 * @author zhangls
 */
@Serializable
data class EmailState(
  val selectedItems: Set<Long> = emptySet(),
  val user: UserModel? = null,
  val isEmailDraftVisible: Boolean = false,
  val isSending: Boolean = false,
  val accounts: List<AccountModel> = emptyList(),
  val draftRecipientIds: Set<Long> = emptySet(),
  val draftSubject: String = "",
  val draftBody: String = "",
) : MviState
