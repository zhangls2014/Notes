package me.zhangls.main.search

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
import kotlinx.coroutines.withContext
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.framework.toast.ToastGlobalNotifier
import me.zhangls.main.search.domain.AvatarSaver
import notes.feature.main.generated.resources.Res
import notes.feature.main.generated.resources.main_msg_save_avatar_failed
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * @author zhangls
 */
@OptIn(ExperimentalMaterial3Api::class)
@KoinViewModel
internal class SearchViewModel(
  savedStateHandle: SavedStateHandle,
  private val emailsRepository: EmailsRepository,
  private val userRepository: UserRepository,
  private val toastGlobalNotifier: ToastGlobalNotifier,
) : MviViewModel<SearchState, SearchIntent>(
  initialState = SearchState(),
  stateSerializer = SearchState.serializer(),
  savedStateHandle = savedStateHandle,
), KoinComponent {
  @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
  val searchResults = state
    .map { it.searchText }
    .flatMapLatest { emailsRepository.searchEmails(it) }
    .flowOn(Dispatchers.IO)
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(DURATION_STOP_SUBSCRIBED))
    .cachedIn(viewModelScope)

  private val avatarSaver: AvatarSaver by inject()

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

  @OptIn(ExperimentalMaterial3Api::class)
  override fun handleIntent(intent: SearchIntent) {
    when (intent) {
      is SearchIntent.UpdateSelectedAvatar -> updateAvatar(intent)
      is SearchIntent.UpdateSearchText -> updateSearchText(intent)
      is SearchIntent.UpdateSearchBarValue -> dispatch(SearchAction.UpdateSearchBarValue(intent.value))
      is SearchIntent.SelectSearchHistory -> selectSearchHistory(intent)
      is SearchIntent.SaveSearchHistory -> saveSearchHistory(intent)
      is SearchIntent.DeleteSearchHistory -> deleteSearchHistory(intent)
    }
  }

  private fun updateAvatar(intent: SearchIntent.UpdateSelectedAvatar) {
    viewModelScope.launch {
      val avatar = intent.avatar ?: return@launch
      val path = withContext(Dispatchers.IO) {
        avatarSaver.save(avatar)
      }
      if (path == null) {
        toastGlobalNotifier.showToast(Res.string.main_msg_save_avatar_failed)
      } else {
        withState {
          user?.avatar?.let {
            // 删除旧的头像
            launch(Dispatchers.IO) {
              avatarSaver.delete(it)
            }
          }
        }
        userRepository.updateAvatar(avatar = path)
      }
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
