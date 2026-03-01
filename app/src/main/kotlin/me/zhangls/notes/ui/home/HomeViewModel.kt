package me.zhangls.notes.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.framework.mvi.ToastGlobalNotifier
import javax.inject.Inject

/**
 * @author zhangls
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val userRepository: UserRepository,
  private val toastGlobalNotifier: ToastGlobalNotifier
) : MviViewModel<HomeState, HomeIntent>(
  initialState = HomeState(greeting = ""),
  stateSerializer = HomeState.serializer(),
  savedStateHandle = savedStateHandle
) {
  init {
    viewModelScope.launch {
      userRepository.userFlow.collect {
        if (it != null) {
          updateState {
            copy(greeting = "Hello, ${it.nickname}!(${it.id})")
          }
        }
      }
    }
  }

  override fun handleIntent(intent: HomeIntent) {
    when (intent) {
      is HomeIntent.ShowToast -> {
        toastGlobalNotifier.showToast(intent.resId)
      }
    }
  }
}