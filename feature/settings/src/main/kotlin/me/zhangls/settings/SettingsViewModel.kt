package me.zhangls.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.data.repository.UserRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.settings.domain.SettingsHandler
import javax.inject.Inject

/**
 * @author zhangls
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
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
          updateState {
            copy(preferences = it)
          }
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
        handler.checkClickSettings(intent.key)?.let { dialog ->
          updateState {
            copy(dialog = dialog)
          }
        }
      }

      is SettingsIntent.DialogCallback -> {
        updateState {
          copy(dialog = null)
        }
        viewModelScope.launch {
          handler.handleDialogCallback(intent.result) {
            sendEffect(it)
          }
        }
      }
    }
  }
}
