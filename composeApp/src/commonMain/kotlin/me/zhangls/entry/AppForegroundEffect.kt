package me.zhangls.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal expect fun rememberConfigurationChangeCheck(): () -> Boolean

@Composable
internal fun AppForegroundEffect(onForegroundChange: (Boolean) -> Unit) {
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  val callback = rememberUpdatedState(onForegroundChange)
  val changingConfigurations = rememberConfigurationChangeCheck()
  DisposableEffect(lifecycle, changingConfigurations) {
    val observer = AppForegroundObserver(changingConfigurations) { callback.value(it) }
    lifecycle.addObserver(observer)
    onDispose { lifecycle.removeObserver(observer) }
  }
}

internal class AppForegroundObserver(
  private val changingConfigurations: () -> Boolean,
  private val onForegroundChange: (Boolean) -> Unit,
) : LifecycleEventObserver {
  override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
    when (event) {
      Lifecycle.Event.ON_START -> onForegroundChange(true)
      Lifecycle.Event.ON_STOP -> if (!changingConfigurations()) onForegroundChange(false)
      else -> Unit
    }
  }
}
