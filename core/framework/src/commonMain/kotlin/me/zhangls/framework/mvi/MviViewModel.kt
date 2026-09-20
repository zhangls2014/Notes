package me.zhangls.framework.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer

/**
 * MVI 基类：Intent → State（+ Effect）单向数据流。
 *
 * State 的跨进程持久化是 **opt-in** 的 —— 默认纯内存态，见 [savedKey]。
 *
 * @author zhangls
 */
abstract class MviViewModel<S : MviState, I : MviIntent>(
  initialState: S,
  stateSerializer: KSerializer<S>,
  savedStateHandle: SavedStateHandle,
  // 安全默认：不保存（纯内存态，进程销毁即丢失）。
  //
  // 默认值刻意取 null 而非某个 key —— State 普遍含敏感数据（登录密码、
  // accessToken / refreshToken）或大对象（分页列表、账户集），一旦写进
  // SavedStateHandle 就会随 instance state 落盘，并可能触发 TransactionTooLarge。
  // "默认持久化 + 各自记得关掉"这种约定必然漏（本项目就漏了登录态与搜索态两处），
  // 而漏掉的后果是静默的凭证泄漏。故改为：确需跨进程恢复时才显式给 key，
  // 且给 key 的那一刻必须确认该 State 不含敏感数据。
  savedKey: String? = null
) : ViewModel() {
  private val _intent = MutableSharedFlow<I>(extraBufferCapacity = 64)

  // saved() 的 key 参数传 null 会按属性名自动派生 key 而非禁用保存，
  // 因此不保存时必须整体不创建 SavedStateHandle 委托，退化为普通内存属性
  private val savedStateDelegate: ReadWriteProperty<Any?, S> =
    if (savedKey != null) {
      savedStateHandle.saved(serializer = stateSerializer, key = savedKey) {
        initialState
      }
    } else {
      EphemeralState(initialState)
    }

  private var savedState by savedStateDelegate
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
    // MutableStateFlow.update 基于 CAS 循环，避免并发"读-改-写"丢更新
    _state.update { state(it) }
  }

  protected fun <R> withState(block: S.() -> R): R {
    return block(_state.value)
  }
}

/**
 * 不持久化场景下的属性委托：普通内存读写。
 */
private class EphemeralState<S>(initialValue: S) : ReadWriteProperty<Any?, S> {
  private var value: S = initialValue

  override fun getValue(thisRef: Any?, property: KProperty<*>): S = value

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: S) {
    this.value = value
  }
}
