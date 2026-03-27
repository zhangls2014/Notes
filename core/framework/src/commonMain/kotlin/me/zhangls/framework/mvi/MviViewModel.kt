package me.zhangls.framework.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer

/**
 * @author zhangls
 */
abstract class MviViewModel<S : MviState, I : MviIntent>(
  initialState: S,
  stateSerializer: KSerializer<S>,
  savedStateHandle: SavedStateHandle,
  // 当保存的 key 为 null 时，则不保存
  savedKey: String? = "state"
) : ViewModel() {
  private val _intent = MutableSharedFlow<I>(extraBufferCapacity = 64)

  private var savedState by savedStateHandle.saved(serializer = stateSerializer, key = savedKey) {
    initialState
  }
  private val _state = MutableStateFlow(savedState)
  val state = _state.asStateFlow()

  private val _effect = MutableSharedFlow<MviEffect>(extraBufferCapacity = 16)
  val effect = _effect.asSharedFlow()


  init {
    viewModelScope.launch {
      _intent.collect {
        handleIntent(it)
      }
    }

    if (savedKey != null) {
      // 订阅 StateFlow 自动保存到 SavedStateHandle
      viewModelScope.launch {
        state.collect {
          savedState = it
        }
      }
    }
  }

  protected abstract fun handleIntent(intent: I)

  fun sendIntent(intent: I) {
    _intent.tryEmit(intent)
  }

  protected fun sendEffect(effect: MviEffect) {
    _effect.tryEmit(effect)
  }

  protected fun updateState(state: S.() -> S) {
    _state.tryEmit(state(_state.value))
  }

  protected fun <R> withState(block: S.() -> R): R {
    return block(_state.value)
  }
}