package me.zhangls.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.framework.toast.ToastGlobalNotifier
import org.koin.core.annotation.KoinViewModel

/**
 * @author zhangls
 */
@KoinViewModel
class AppViewModel(
  savedStateHandle: SavedStateHandle,
  toastGlobalNotifier: ToastGlobalNotifier,
  userRepository: UserRepository,
  settingsRepository: SettingsRepository
) : MviViewModel<AppState, AppIntent>(
  initialState = AppState(null),
  stateSerializer = AppState.serializer(),
  savedStateHandle = savedStateHandle,
  // 显式开启持久化：isLogin 为 null 时 AppNavHost 直接 return（不渲染任何 UI），
  // 恢复它可避免进程重建后多出一帧空白。
  // AppState 只含登录标志与主题 / 语言等非敏感展示值（真实取值来自 DataStore，不在本 State 里）
  savedKey = "state",
) {
  /**
   * 全局 Toast 队列
   */
  val toast = toastGlobalNotifier.toast
    .shareIn(viewModelScope, SharingStarted.WhileSubscribed())

  override fun handleIntent(intent: AppIntent) {}

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
