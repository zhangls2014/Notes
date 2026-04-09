package me.zhangls.email.search

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBarValue
import com.mohamedrejeb.calf.io.KmpFile
import me.zhangls.framework.mvi.MviIntent

/**
 * @author zhangls
 */
sealed class SearchIntent : MviIntent {
  data class UpdateSelectedAvatar(val avatar: KmpFile?) : SearchIntent()
  data class UpdateSearchText(val keyword: String) : SearchIntent()
  @OptIn(ExperimentalMaterial3Api::class)
  data class UpdateSearchBarValue(val value: SearchBarValue) : SearchIntent()
  data class SelectSearchHistory(val keyword: String) : SearchIntent()
  data class SaveSearchHistory(val keyword: String) : SearchIntent()
  data class DeleteSearchHistory(val keyword: String) : SearchIntent()
}
