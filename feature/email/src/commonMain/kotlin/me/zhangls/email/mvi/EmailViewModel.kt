package me.zhangls.email.mvi

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
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
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * @author zhangls
 */
@KoinViewModel
internal class EmailViewModel(
  savedStateHandle: SavedStateHandle,
  private val emailsRepository: EmailsRepository,
  private val userRepository: UserRepository,
  private val ioDispatcher: CoroutineDispatcher,
  private val toastGlobalNotifier: ToastGlobalNotifier,
) : MviViewModel<EmailState, EmailIntent>(
  initialState = EmailState(),
  stateSerializer = EmailState.serializer(),
  savedStateHandle = savedStateHandle,
) {
  val draftSubjectInput = TextFieldState()
  val draftBodyInput = TextFieldState()

  // 首页邮件列表，缓存分页数据
  val emailPaging = emailsRepository.getEmailPaging()
    .flowOn(ioDispatcher)
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(DURATION_STOP_SUBSCRIBED))
    .cachedIn(viewModelScope)

  val emailFavoritePaging = emailsRepository.getEmailFavoritePaging()
    .flowOn(ioDispatcher)
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(DURATION_STOP_SUBSCRIBED))
    .cachedIn(viewModelScope)

  fun getThreadEmails(parentEmailId: Long): Flow<PagingData<EmailModel>> =
    emailsRepository.getThreadEmailsById(parentEmailId)
      .flowOn(ioDispatcher)
      .shareIn(viewModelScope, SharingStarted.WhileSubscribed(DURATION_STOP_SUBSCRIBED))
      .cachedIn(viewModelScope)

  private val detailRetry = MutableStateFlow(0)

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  fun getEmail(emailId: Long): Flow<EmailDetailState> = detailRetry.flatMapLatest {
    emailsRepository.getEmail(emailId).asEmailDetailState()
  }
    .flowOn(ioDispatcher)
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(DURATION_STOP_SUBSCRIBED), replay = 1)

  private var allAccountsJob: Job? = null

  companion object {
    private const val DURATION_STOP_SUBSCRIBED = 5000L

    /**
     * 首页列表所用的 ViewModel key。
     *
     * 判据是"一屏一个实例"：分页缓存 / 多选 / 草稿这些"屏幕状态"必须各归其主。首页与收藏页
     * 现在各自是独立的导航目的地，`ViewModelStore` 天然分开；显式给 key 是为了不依赖
     * "同一个 store 里恰好只有一个列表"这个偶然事实 —— 早先两个 tab 同属一个 NavEntry 时，
     * 共用实例表现为跨 tab 串味，只是被收藏页不开放多选掩盖着。
     */
    const val KEY_HOME = "email_view_model_home"

    /** 收藏列表所用的 ViewModel key，与 [KEY_HOME] 成对，理由见 [KEY_HOME]。 */
    const val KEY_FAVORITES = "email_view_model_favorites"
  }

  init {
    viewModelScope.launch {
      snapshotFlow { draftSubjectInput.text.toString() to draftBodyInput.text.toString() }.collect { (subject, body) ->
        if (!state.value.isSending) {
          dispatch(EmailAction.UpdateDraftSubject(subject))
          dispatch(EmailAction.UpdateDraftBody(body))
        }
      }
    }
    viewModelScope.launch {
      userRepository.userFlow.collectLatest {
        dispatch(EmailAction.UpdateUser(it))
      }
    }
  }

  private fun dispatch(action: EmailAction) {
    updateState { EmailReducer.reduce(this, action) }
    if (action is EmailAction.UpdateDraftSubject && draftSubjectInput.text.toString() != state.value.draftSubject) {
      draftSubjectInput.setTextAndPlaceCursorAtEnd(state.value.draftSubject)
    }
    if (action is EmailAction.UpdateDraftBody && draftBodyInput.text.toString() != state.value.draftBody) {
      draftBodyInput.setTextAndPlaceCursorAtEnd(state.value.draftBody)
    }
    if (action == EmailAction.ClearDraft || action is EmailAction.UpdateUser || action is EmailAction.PrepareDraft) {
      val draft = state.value
      androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
        draftSubjectInput.setTextAndPlaceCursorAtEnd(draft.draftSubject)
        draftBodyInput.setTextAndPlaceCursorAtEnd(draft.draftBody)
      }
    }
  }

  override fun handleIntent(intent: EmailIntent) {
    when (intent) {
      is EmailIntent.ShowToast -> toastGlobalNotifier.showToast(intent.res)

      is EmailIntent.UpdateSelectedEmail -> dispatch(EmailAction.UpdateSelectedEmail(intent.emailId))

      EmailIntent.RetryDetail -> detailRetry.value++

      is EmailIntent.ComposeFrom -> {
        startCollectAccounts()
        dispatch(prepareEmailDraft(intent.email, intent.mode))
      }

      EmailIntent.OpenDraft -> {
        startCollectAccounts()
        dispatch(EmailAction.SetDraftVisible(true))
      }

      EmailIntent.CloseDraft -> {
        dispatch(EmailAction.SetDraftVisible(false))
        cancelCollectAccounts()
      }

      EmailIntent.ClearDraft -> {
        pendingDraft = null
        dispatch(EmailAction.ClearDraft)
        dispatch(EmailAction.SetDraftVisible(false))
        cancelCollectAccounts()
      }

      EmailIntent.SubmitDraft -> submitDraft()

      is EmailIntent.UpdateDraftRecipient -> dispatch(
        EmailAction.UpdateDraftRecipients(
          intent.recipientId,
          intent.selected,
        ),
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

  private var pendingDraft: EmailDraft? = null

  @OptIn(ExperimentalUuidApi::class)
  private fun submitDraft() {
    if (!state.value.isSending) {
      dispatch(EmailAction.UpdateDraftSubject(draftSubjectInput.text.toString()))
      dispatch(EmailAction.UpdateDraftBody(draftBodyInput.text.toString()))
    }
    withState {
      if (isSending) return@withState
      if (draftRecipientIds.isEmpty()) {
        toastGlobalNotifier.showToast(Res.string.email_msg_recipient_required)
        return@withState
      }
      if (draftBody.isBlank()) {
        toastGlobalNotifier.showToast(Res.string.email_msg_body_required)
        return@withState
      }

      viewModelScope.launch {
        dispatch(EmailAction.SetDraftVisible(false))
        dispatch(EmailAction.SetSending(true))
        try {
          // 服务端决定发件身份；同一内容重试复用 operationId，防止响应丢失后重复发送。
          val draft = EmailDraft(
            recipientIds = draftRecipientIds,
            subject = draftSubject,
            body = draftBody,
            createdAt = "",
            mode = draftMode,
            sourceId = draftSourceId,
          )
          val pending = pendingDraft?.takeIf { it.copy(operationId = "") == draft }
            ?: draft.copy(operationId = Uuid.random().toString()).also { pendingDraft = it }
          emailsRepository.insertDraft(pending)
          pendingDraft = null
          dispatch(EmailAction.ClearDraft)
        } catch (cancelled: CancellationException) {
          throw cancelled
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
    launchMailOperation { emailsRepository.toggleFavorite(intent.emailId) }
  }

  private fun updateMultiFavorite(isImportant: Boolean) {
    withState {
      launchMailOperation {
        emailsRepository.updateIsFavorite(selectedItems, isImportant)
        dispatch(EmailAction.ClearSelectedEmail)
      }
    }
  }

  private fun deleteMulti() {
    withState {
      launchMailOperation {
        emailsRepository.deleteEmails(selectedItems)
        dispatch(EmailAction.ClearSelectedEmail)
      }
    }
  }

  private fun startCollectAccounts() {
    allAccountsJob?.cancel()
    allAccountsJob = viewModelScope.launch {
      emailsRepository.getAllAccounts().catch { failure ->
        if (failure is CancellationException) throw failure
        toastGlobalNotifier.showToast(Res.string.email_msg_email_save_failed)
      }.collectLatest { accounts ->
        dispatch(EmailAction.UpdateAllAccounts(accounts))
      }
    }
  }

  private fun launchMailOperation(block: suspend () -> Unit) {
    viewModelScope.launch {
      try {
        block()
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        toastGlobalNotifier.showToast(Res.string.email_msg_email_save_failed)
      }
    }
  }

  private fun cancelCollectAccounts() {
    allAccountsJob?.cancel()
    allAccountsJob = null
    dispatch(EmailAction.UpdateAllAccounts(emptyList()))
  }
}
