package me.zhangls.email.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.zhangls.data.model.EmailDraft
import me.zhangls.data.model.EmailModel
import me.zhangls.data.repository.EmailsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.framework.toast.ToastGlobalNotifier
import notes.feature.email.generated.resources.Res
import notes.feature.email.generated.resources.email_msg_body_required
import notes.feature.email.generated.resources.email_msg_email_save_failed
import notes.feature.email.generated.resources.email_msg_recipient_required
import notes.feature.email.generated.resources.email_time_just_now
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.KoinViewModel

/**
 * @author zhangls
 */
@KoinViewModel
internal class EmailViewModel(
  savedStateHandle: SavedStateHandle,
  private val emailsRepository: EmailsRepository,
  private val userRepository: UserRepository,
  private val toastGlobalNotifier: ToastGlobalNotifier,
) : MviViewModel<EmailState, EmailIntent>(
  initialState = EmailState(),
  stateSerializer = EmailState.serializer(),
  savedStateHandle = savedStateHandle,
) {
  // 首页邮件列表，缓存分页数据
  val emailPaging = emailsRepository.getEmailPaging()
    .flowOn(Dispatchers.IO)
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(DURATION_STOP_SUBSCRIBED))
    .cachedIn(viewModelScope)

  val emailFavoritePaging = emailsRepository.getEmailFavoritePaging()
    .flowOn(Dispatchers.IO)
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(DURATION_STOP_SUBSCRIBED))
    .cachedIn(viewModelScope)

  fun getThreadEmails(parentEmailId: Long): Flow<PagingData<EmailModel>> {
    return emailsRepository.getThreadEmailsById(parentEmailId)
      .flowOn(Dispatchers.IO)
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed(DURATION_STOP_SUBSCRIBED))
      .cachedIn(viewModelScope)
  }

  fun getEmail(emailId: Long): Flow<EmailModel?> {
    return emailsRepository.getEmail(emailId)
      .flowOn(Dispatchers.IO)
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed(DURATION_STOP_SUBSCRIBED))
  }

  private var allAccountsJob: Job? = null


  companion object {
    private const val DURATION_STOP_SUBSCRIBED = 5000L
  }

  init {
    viewModelScope.launch {
      userRepository.userFlow.collectLatest {
        dispatch(EmailAction.UpdateUser(it))
      }
    }
  }

  private fun dispatch(action: EmailAction) {
    updateState { EmailReducer.reduce(this, action) }
  }

  override fun handleIntent(intent: EmailIntent) {
    when (intent) {
      is EmailIntent.ShowToast -> toastGlobalNotifier.showToast(intent.res)
      is EmailIntent.UpdateSelectedEmail -> dispatch(EmailAction.UpdateSelectedEmail(intent.emailId))
      EmailIntent.OpenDraft -> {
        startCollectAccounts()
        dispatch(EmailAction.SetDraftVisible(true))
      }

      EmailIntent.CloseDraft -> {
        dispatch(EmailAction.SetDraftVisible(false))
        cancelCollectAccounts()
      }
      EmailIntent.ClearDraft -> {
        dispatch(EmailAction.ClearDraft)
        dispatch(EmailAction.SetDraftVisible(false))
        cancelCollectAccounts()
      }
      EmailIntent.SubmitDraft -> submitDraft()
      is EmailIntent.UpdateDraftRecipient -> dispatch(
        EmailAction.UpdateDraftRecipients(
          intent.recipientId,
          intent.selected
        )
      )

      is EmailIntent.UpdateDraftSubject -> dispatch(EmailAction.UpdateDraftSubject(intent.subject))
      is EmailIntent.UpdateDraftBody -> dispatch(EmailAction.UpdateDraftBody(intent.body))
      is EmailIntent.UpdateFavorite -> updateFavorite(intent)
      is EmailIntent.MultiFavorite -> updateMultiFavorite(true)
      is EmailIntent.MultiCancelFavorite -> updateMultiFavorite(false)
      is EmailIntent.MultiDelete -> deleteMulti()
      EmailIntent.ClearSelectedEmail -> dispatch(EmailAction.ClearSelectedEmail)
    }
  }

  private fun submitDraft() {
    withState {
      if (isSending) return@withState
      if (draftRecipientIds.isEmpty()) {
        toastGlobalNotifier.showToast(Res.string.email_msg_recipient_required)
        return@withState
      }
      if (draftBody.isEmpty()) {
        toastGlobalNotifier.showToast(Res.string.email_msg_body_required)
        return@withState
      }


      viewModelScope.launch {
        dispatch(EmailAction.SetDraftVisible(false))
        dispatch(EmailAction.SetSending(true))
        try {
          // 收件人 ID 的落盘编码与"默认发件账户"的选取都在数据层内部完成
          emailsRepository.insertDraft(
            EmailDraft(
              recipientIds = draftRecipientIds,
              subject = draftSubject,
              body = draftBody,
              createdAt = getString(Res.string.email_time_just_now),
            )
          )
          dispatch(EmailAction.ClearDraft)
        } catch (_: Exception) {
          dispatch(EmailAction.SetDraftVisible(true))
          toastGlobalNotifier.showToast(Res.string.email_msg_email_save_failed)
        } finally {
          dispatch(EmailAction.SetSending(false))
        }
      }
    }
  }

  private fun updateFavorite(intent: EmailIntent.UpdateFavorite) {
    viewModelScope.launch {
      emailsRepository.toggleFavorite(intent.emailId)
    }
  }

  private fun updateMultiFavorite(isImportant: Boolean) {
    withState {
      viewModelScope.launch {
        emailsRepository.updateIsFavorite(selectedItems, isImportant)
        dispatch(EmailAction.ClearSelectedEmail)
      }
    }
  }

  private fun deleteMulti() {
    withState {
      viewModelScope.launch {
        emailsRepository.deleteEmails(selectedItems)
        dispatch(EmailAction.ClearSelectedEmail)
      }
    }
  }

  private fun startCollectAccounts() {
    allAccountsJob = viewModelScope.launch {
      emailsRepository.getAllAccounts().collectLatest { accounts ->
        dispatch(EmailAction.UpdateAllAccounts(accounts))
      }
    }
  }

  private fun cancelCollectAccounts() {
    allAccountsJob?.cancel()
    allAccountsJob = null
    dispatch(EmailAction.UpdateAllAccounts(emptyList()))
  }
}
