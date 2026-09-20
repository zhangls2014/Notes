package me.zhangls.email.search

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarValue
import kotlinx.serialization.Serializable
import me.zhangls.data.model.UserModel
import me.zhangls.framework.mvi.MviState

/**
 * 含 [UserModel]（accessToken / refreshToken）—— 属敏感数据，依赖 MviViewModel 的默认
 * 非持久化行为，不要为它指定 savedKey。
 *
 * @author zhangls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Serializable
data class SearchState(
  val user: UserModel? = null,
  val searchText: String = "",
  val searchBarValue: SearchBarValue = SearchBarValue.Collapsed,
  val searchHistory: List<String> = emptyList()
) : MviState
