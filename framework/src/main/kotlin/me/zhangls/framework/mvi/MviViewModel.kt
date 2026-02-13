package me.zhangls.framework.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * @author zhangls
 */
abstract class MviViewModel<S : MviState, I : MviIntent>(initialState: S) : ViewModel() {
  private val _intent = MutableSharedFlow<I>(extraBufferCapacity = 64)

  private val _state = MutableStateFlow(initialState)
  val state = _state.asStateFlow()

  private val _effect = MutableSharedFlow<MviEffect>(extraBufferCapacity = 16)
  val effect = _effect.asSharedFlow()


  init {
    viewModelScope.launch {
      _intent.collect {
        handleIntent(it)
      }
    }
  }

  abstract fun handleIntent(intent: I)

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