package me.zhangls.main.search

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarValue
import me.zhangls.data.model.UserModel
import me.zhangls.framework.mvi.MviAction

/**
 * @author zhangls
 */
internal sealed interface SearchAction : MviAction {
  data class UpdateUser(val user: UserModel?) : SearchAction
  data class UpdateSearchHistory(val history: List<String>) : SearchAction
  data class UpdateSearchText(val text: String) : SearchAction

  @OptIn(ExperimentalMaterial3Api::class)
  data class UpdateSearchBarValue(val value: SearchBarValue) : SearchAction
}
