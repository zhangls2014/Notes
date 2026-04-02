package me.zhangls.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.framework.toast.ToastGlobalNotifier
import me.zhangls.main.domain.AvatarSaver
import notes.feature.main.generated.resources.Res
import notes.feature.main.generated.resources.main_msg_save_avatar_failed
import org.koin.core.annotation.KoinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * @author zhangls
 */
@KoinViewModel
class EmailViewModel(
  savedStateHandle: SavedStateHandle,
  private val emailsRepository: EmailsRepository,
  private val userRepository: UserRepository,
  private val toastGlobalNotifier: ToastGlobalNotifier,
) : MviViewModel<EmailState, EmailIntent>(
  initialState = EmailState(),
  stateSerializer = EmailState.serializer(),
  savedStateHandle = savedStateHandle,
), KoinComponent {
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

  @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
  val searchResults = currentQuery
    .debounce(300)
    .map { it.trim() }
    .flatMapLatest { emailsRepository.searchEmails(it) }
    .flowOn(Dispatchers.IO)
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))
    .cachedIn(viewModelScope)

  fun getEmail(emailId: Long): Flow<EmailConvertModel?> {
    return emailsRepository.getEmail(emailId)
      .flowOn(Dispatchers.IO)
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000))
  }

  private val avatarSaver: AvatarSaver by inject()

  init {
    viewModelScope.launch {
      userRepository.userFlow.collectLatest {
        dispatch(EmailAction.UpdateUser(it))
      }
    }
  }

  override fun handleIntent(intent: EmailIntent) {
    when (intent) {
      is EmailIntent.ShowToast -> {
        toastGlobalNotifier.showToast(intent.res)
      }

      is EmailIntent.UpdateSelectedEmail -> {
        dispatch(EmailAction.UpdateSelectedEmail(intent.emailId))
      }

      is EmailIntent.UpdateSearchText -> {
        currentQuery.value = intent.text.toString()
        dispatch(EmailAction.UpdateSearchText(intent.text.toString()))
      }

      is EmailIntent.UpdateFavorite -> {
        viewModelScope.launch {
          val entity = emailsRepository.getEmailById(intent.emailId) ?: return@launch
          emailsRepository.insertEmail(entity.copy(isImportant = entity.isImportant.not()))
        }
      }

      is EmailIntent.MultiFavorite -> {
        withState {
          viewModelScope.launch {
            emailsRepository.updateIsFavorite(selectedItems, true)
            dispatch(EmailAction.ClearSelectedEmail)
          }
        }
      }

      is EmailIntent.MultiCancelFavorite -> {
        withState {
          viewModelScope.launch {
            emailsRepository.updateIsFavorite(selectedItems, false)
            dispatch(EmailAction.ClearSelectedEmail)
          }
        }
      }

      is EmailIntent.MultiDelete -> {
        withState {
          viewModelScope.launch {
            emailsRepository.deleteEmails(selectedItems)
            dispatch(EmailAction.ClearSelectedEmail)
          }
        }
      }

      EmailIntent.ClearSelectedEmail -> {
        dispatch(EmailAction.ClearSelectedEmail)
      }

      is EmailIntent.UpdateSelectedAvatar -> {
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
    }
  }

  private fun dispatch(action: EmailAction) {
    updateState { EmailReducer.reduce(this, action) }
  }
}