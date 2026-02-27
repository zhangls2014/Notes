package me.zhangls.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.framework.mvi.ToastGlobalNotifier
import javax.inject.Inject

/**
 * @author zhangls
 */
@HiltViewModel
class MainViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  toastGlobalNotifier: ToastGlobalNotifier,
  private val userRepository: UserRepository,
) : MviViewModel<MainState, MainIntent>(
  initialState = MainState(null),
  stateSerializer = MainState.serializer(),
  savedStateHandle = savedStateHandle
) {
  /**
   * 全局 Toast 队列
   */
  val toast = toastGlobalNotifier.toast
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

  override fun handleIntent(intent: MainIntent) {
  }

  init {
    viewModelScope.launch {
      userRepository.userFlow.collect {
        updateState { copy(isLogin = it != null) }
      }
    }
  }
}