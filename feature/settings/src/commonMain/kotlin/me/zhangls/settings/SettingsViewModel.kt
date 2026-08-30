package me.zhangls.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.settings.domain.SettingsHandler
import org.koin.core.annotation.KoinViewModel

/**
 * @author zhangls
 */
@KoinViewModel
class SettingsViewModel(
  savedStateHandle: SavedStateHandle,
  userRepository: UserRepository,
  private val settingsRepository: SettingsRepository,
) : MviViewModel<SettingsState, SettingsIntent>(
  initialState = SettingsState(),
  stateSerializer = SettingsState.serializer(),
  savedStateHandle = savedStateHandle
) {
  private val handler = SettingsHandler(userRepository)

  init {
    // 订阅设置数据源，同步到 State
    viewModelScope.launch {
      settingsRepository.settingsFlow
        .map { SettingsAction.UpdateSettings(it) }
        .collectLatest {
          dispatch(it)
        }
    }
  }

  override fun handleIntent(intent: SettingsIntent) {
    when (intent) {
      is SettingsIntent.UpdateDynamicColor -> {
        viewModelScope.launch { settingsRepository.updateDynamicColor(intent.value) }
      }

      is SettingsIntent.UpdateDarkTheme -> {
        viewModelScope.launch { settingsRepository.updateDarkTheme(intent.value) }
      }

      is SettingsIntent.UpdateFontSize -> {
        viewModelScope.launch { settingsRepository.updateFontSize(intent.value) }
      }

      is SettingsIntent.UpdateAppLanguage -> {
        viewModelScope.launch { settingsRepository.updateAppLanguage(intent.value) }
      }

      SettingsIntent.ClickLogout -> {
        dispatch(SettingsAction.ShowDialog(handler.createLogoutDialog()))
      }

      is SettingsIntent.DialogCallback -> {
        dispatch(SettingsAction.DismissDialog)
        viewModelScope.launch {
          handler.handleDialogCallback(intent.result)?.let { sendEffect(it) }
        }
      }
    }
  }

  private fun dispatch(action: SettingsAction) {
    updateState { SettingsReducer.reduce(this, action) }
  }
}
