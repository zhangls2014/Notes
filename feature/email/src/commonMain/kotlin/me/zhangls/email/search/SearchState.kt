package me.zhangls.email.search

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarValue
import kotlinx.serialization.Serializable
import me.zhangls.data.model.UserModel
import me.zhangls.framework.mvi.MviState

/**
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
