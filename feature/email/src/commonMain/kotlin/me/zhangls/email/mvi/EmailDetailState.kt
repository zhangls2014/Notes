package me.zhangls.email.mvi

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import me.zhangls.data.model.EmailModel

internal sealed interface EmailDetailState {
  data object Loading : EmailDetailState
  data object Failed : EmailDetailState
  data object NotFound : EmailDetailState
  data class Loaded(val email: EmailModel) : EmailDetailState
}

internal fun Flow<EmailModel?>.asEmailDetailState(): Flow<EmailDetailState> =
  map<EmailModel?, EmailDetailState> { email ->
    if (email == null) EmailDetailState.NotFound else EmailDetailState.Loaded(email)
  }.onStart {
    emit(EmailDetailState.Loading)
  }.catch { failure ->
    if (failure is CancellationException) throw failure
    emit(EmailDetailState.Failed)
  }
