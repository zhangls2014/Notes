package me.zhangls.notes.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import javax.inject.Inject

/**
 * @author zhangls
 */
@HiltViewModel
class MainViewModel @Inject constructor(
  private val userRepository: UserRepository,
  savedStateHandle: SavedStateHandle
) : MviViewModel<MainState, MainIntent>(
  initialState = MainState(null),
  stateSerializer = MainState.serializer(),
  savedStateHandle = savedStateHandle
) {
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