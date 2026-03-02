package me.zhangls.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.zhangls.data.repository.SettingsRepository
import me.zhangls.framework.mvi.MviViewModel
import me.zhangls.settings.domain.SettingsConverter
import javax.inject.Inject

/**
 * @author zhangls
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val settingsRepository: SettingsRepository,
) : MviViewModel<SettingsState, SettingsIntent>(
  initialState = SettingsState(),
  stateSerializer = SettingsState.serializer(),
  savedStateHandle = savedStateHandle
) {
  private val converter = SettingsConverter()

  init {
    viewModelScope.launch {
      settingsRepository.settingsFlow
        .map { converter.mapToPreferences(it) }
        .collectLatest {
          updateState {
            SettingsState(preferences = it)
          }
        }
    }
  }

  override fun handleIntent(intent: SettingsIntent) {
    when (intent) {
      is SettingsIntent.UpdateSettings<*> -> {
        viewModelScope.launch {
          converter.updateSettings(settingsRepository, intent.key, intent.result)
        }
      }
    }
  }
}

