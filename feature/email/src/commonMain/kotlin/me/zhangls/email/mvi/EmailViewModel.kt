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
import me.zhangls.data.database.entity.EmailConvertModel
import me.zhangls.data.database.entity.EmailEntity
import me.zhangls.data.database.entity.RecipientIdsCodec
import me.zhangls.data.model.toDomain
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
  // EmailState 含 UserModel（带 accessToken/refreshToken）与账户列表，
  // 持久化到 SavedStateHandle 会把敏感凭证写进进程恢复 Bundle，且有大对象
  // TransactionTooLarge 风险；列表数据可从 Room 重新加载，纯内存态即可
  savedKey = null,
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

  fun getThreadEmails(parentEmailId: Long): Flow<PagingData<EmailConvertModel>> {
    return emailsRepository.getThreadEmailsById(parentEmailId)
      .flowOn(Dispatchers.IO)
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed(DURATION_STOP_SUBSCRIBED))
      .cachedIn(viewModelScope)
  }

  fun getEmail(emailId: Long): Flow<EmailConvertModel?> {
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


      val recipientIds = RecipientIdsCodec.encode(draftRecipientIds)
      val subject = draftSubject
      val body = draftBody

      viewModelScope.launch {
        dispatch(EmailAction.SetDraftVisible(false))
        dispatch(EmailAction.SetSending(true))
        try {
          val draft = EmailEntity(
            id = 0L,
            senderId = emailsRepository.getDefaultAccount()?.id ?: return@launch,
            recipientIds = recipientIds,
            subject = subject,
            body = body,
            createdAt = getString(Res.string.email_time_just_now),
          )
          emailsRepository.insertEmail(draft)
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
      val entity = emailsRepository.getEmailById(intent.emailId) ?: return@launch
      // 走 UPDATE 语句，避免用 INSERT OR REPLACE 模拟更新（会触发 REPLACE
      // 语义的删行重建，开销更大且可能触发级联行为）
      emailsRepository.updateIsFavorite(setOf(intent.emailId), entity.isImportant.not())
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
        val models = accounts.map { it.toDomain() }
        dispatch(EmailAction.UpdateAllAccounts(models))
      }
    }
  }

  private fun cancelCollectAccounts() {
    allAccountsJob?.cancel()
    allAccountsJob = null
    dispatch(EmailAction.UpdateAllAccounts(emptyList()))
  }
}
