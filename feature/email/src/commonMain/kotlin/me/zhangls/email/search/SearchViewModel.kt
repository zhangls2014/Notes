package me.zhangls.email.search

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import org.koin.core.annotation.KoinViewModel

/**
 * @author zhangls
 */
// 类级 @OptIn 必需：SearchState/SearchIntent 使用了实验性 SearchBarValue 类型
@OptIn(ExperimentalMaterial3Api::class)
@KoinViewModel
internal class SearchViewModel(
  savedStateHandle: SavedStateHandle,
  private val emailsRepository: EmailsRepository,
  private val userRepository: UserRepository,
) : MviViewModel<SearchState, SearchIntent>(
  initialState = SearchState(),
  stateSerializer = SearchState.serializer(),
  savedStateHandle = savedStateHandle,
) {
  @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
  val searchResults = state
    .map { it.searchText }
    .flatMapLatest { emailsRepository.searchEmails(it) }
    .flowOn(Dispatchers.IO)
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(DURATION_STOP_SUBSCRIBED))
    .cachedIn(viewModelScope)

  internal companion object {
    const val DURATION_SEARCH_DEBOUNCE = 300L
    const val DURATION_STOP_SUBSCRIBED = 5000L
  }

  init {
    viewModelScope.launch {
      userRepository.userFlow.collectLatest {
        dispatch(SearchAction.UpdateUser(it))
        dispatch(SearchAction.UpdateSearchHistory(it?.emailSearchHistory ?: emptyList()))
      }
    }
  }

  private fun dispatch(action: SearchAction) {
    updateState { SearchReducer.reduce(this, action) }
  }

  override fun handleIntent(intent: SearchIntent) {
    when (intent) {
      is SearchIntent.UpdateSearchText -> updateSearchText(intent)
      is SearchIntent.UpdateSearchBarValue -> dispatch(SearchAction.UpdateSearchBarValue(intent.value))
      is SearchIntent.SelectSearchHistory -> selectSearchHistory(intent)
      is SearchIntent.SaveSearchHistory -> saveSearchHistory(intent)
      is SearchIntent.DeleteSearchHistory -> deleteSearchHistory(intent)
    }
  }

  private fun updateSearchText(intent: SearchIntent.UpdateSearchText) {
    val keyword = intent.keyword.trim()
    dispatch(SearchAction.UpdateSearchText(keyword))
  }

  private fun selectSearchHistory(intent: SearchIntent.SelectSearchHistory) {
    val keyword = intent.keyword.trim()
    if (keyword.isEmpty()) return
    dispatch(SearchAction.UpdateSearchText(keyword))
  }

  private fun saveSearchHistory(intent: SearchIntent.SaveSearchHistory) {
    viewModelScope.launch {
      userRepository.updateEmailSearchHistory(intent.keyword)
    }
  }

  private fun deleteSearchHistory(intent: SearchIntent.DeleteSearchHistory) {
    viewModelScope.launch {
      userRepository.deleteEmailSearchHistory(intent.keyword)
    }
  }
}
