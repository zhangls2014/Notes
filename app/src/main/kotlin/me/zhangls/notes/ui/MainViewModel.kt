package me.zhangls.notes.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.framework.mvi.ToastGlobalNotifier
import org.koin.core.annotation.KoinViewModel

/**
 * @author zhangls
 */
@KoinViewModel
class MainViewModel(
  savedStateHandle: SavedStateHandle,
  toastGlobalNotifier: ToastGlobalNotifier,
  userRepository: UserRepository,
  settingsRepository: SettingsRepository
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
      userRepository.userFlow.collectLatest {
        updateState { copy(isLogin = it != null) }
      }
    }
    viewModelScope.launch {
      settingsRepository.settingsFlow.collectLatest {
        updateState {
          copy(
            dynamicColor = it.dynamicColor,
            darkTheme = it.darkTheme,
            fontSize = it.fontSize,
            appLanguage = it.appLanguage
          )
        }
      }
    }
  }
}
