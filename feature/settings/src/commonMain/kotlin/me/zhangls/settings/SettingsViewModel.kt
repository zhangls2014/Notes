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
import me.zhangls.settings.domain.SettingsHandler.ClickAction
import org.koin.core.annotation.KoinViewModel

/**
 * @author zhangls
 */
@KoinViewModel
class SettingsViewModel(
  savedStateHandle: SavedStateHandle,
  userRepository: UserRepository,
  settingsRepository: SettingsRepository,
) : MviViewModel<SettingsState, SettingsIntent>(
  initialState = SettingsState(),
  stateSerializer = SettingsState.serializer(),
  savedStateHandle = savedStateHandle,
  // 不保存状态
  savedKey = null
) {
  private val handler = SettingsHandler(userRepository, settingsRepository)

  init {
    // SettingsModel 转换为 List<Preference>
    viewModelScope.launch {
      settingsRepository.settingsFlow
        .map { handler.mapToPreferences(it) }
        .collectLatest {
          dispatch(SettingsAction.UpdatePreferences(it))
        }
    }
  }

  override fun handleIntent(intent: SettingsIntent) {
    when (intent) {
      is SettingsIntent.UpdateSettings<*> -> {
        viewModelScope.launch {
          handler.updateSettings(intent.key, intent.result)
        }
      }

      is SettingsIntent.ClickSettings -> {
        viewModelScope.launch {
          when (val action = handler.checkClickSettings(intent.key)) {
            ClickAction.None -> {}
            is ClickAction.ShowDialog -> dispatch(SettingsAction.ShowDialog(action.dialog))
            is ClickAction.EmitEffect -> sendEffect(action.effect)
          }
        }
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
