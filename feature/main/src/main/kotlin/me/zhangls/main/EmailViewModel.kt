package me.zhangls.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.model.toDomain
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.framework.mvi.ToastGlobalNotifier
import javax.inject.Inject

/**
 * @author zhangls
 */
@HiltViewModel
class EmailViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val emailsRepository: EmailsRepository,
  private val toastGlobalNotifier: ToastGlobalNotifier
) : MviViewModel<EmailState, EmailIntent>(
  initialState = EmailState(),
  stateSerializer = EmailState.serializer(),
  savedStateHandle = savedStateHandle,
) {
  // 首页邮件列表，缓存分页数据
  val emailPaging = emailsRepository.getEmailPaging()
    .flowOn(Dispatchers.IO)
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))
    .cachedIn(viewModelScope)

  val emailFavoritePaging = emailsRepository.getEmailFavoritePaging()
    .flowOn(Dispatchers.IO)
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))
    .cachedIn(viewModelScope)

  fun getThreadEmails(parentEmailId: Long): Flow<PagingData<EmailConvertModel>> {
    return emailsRepository.getThreadEmailsById(parentEmailId)
      .flowOn(Dispatchers.IO)
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))
      .cachedIn(viewModelScope)
  }

  private val currentQuery = MutableStateFlow("")

  @OptIn(ExperimentalCoroutinesApi::class)
  val searchResults = currentQuery
    .debounce(300)
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .flatMapLatest { emailsRepository.searchEmails(it) }
    .flowOn(Dispatchers.IO)
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))
    .cachedIn(viewModelScope)

  private val ownerAccount = emailsRepository.getOwnerAccount()
    .flowOn(Dispatchers.IO)
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))

  fun getEmail(emailId: Long): Flow<EmailConvertModel?> {
    return emailsRepository.getEmail(emailId)
      .flowOn(Dispatchers.IO)
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))
  }

  init {
    viewModelScope.launch {
      ownerAccount.collectLatest {
        updateState { copy(ownerAccount = it?.toDomain()) }
      }
    }
  }

  override fun handleIntent(intent: EmailIntent) {
    when (intent) {
      is EmailIntent.ShowToast -> {
        toastGlobalNotifier.showToast(intent.resId)
      }

      is EmailIntent.UpdateSelectedEmail -> {
        updateState {
          val newSelectedItems = if (selectedItems.contains(intent.emailId)) {
            selectedItems - intent.emailId
          } else {
            selectedItems + intent.emailId
          }
          copy(selectedItems = newSelectedItems)
        }
      }

      is EmailIntent.UpdateSearchText -> {
        currentQuery.value = intent.text.toString()
        updateState { copy(searchText = intent.text) }
      }

      is EmailIntent.UpdateFavorite -> {
        viewModelScope.launch {
          val entity = emailsRepository.getEmailById(intent.emailId) ?: return@launch
          emailsRepository.insertEmail(entity.copy(isImportant = entity.isImportant.not()))
        }
      }
    }
  }
}