package me.zhangls.theme.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

@Composable
fun Toast(toastData: ToastData) {
  val message = toastData.message

  if (message.isNotBlank()) {
    Box(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.inverseSurface, shape = MaterialTheme.shapes.small)
        .padding(12.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(message, color = MaterialTheme.colorScheme.inverseOnSurface)
    }
  }
}

@Composable
fun ToastHost(
  toastState: ToastState,
  toast: @Composable (ToastData) -> Unit = { Toast(it) }
) {
  // 当前显示的 Toast
  val current = toastState.currentToast
  val visible = remember { MutableTransitionState(true) }

  LaunchedEffect(current) {
    current ?: return@LaunchedEffect
    visible.targetState = true
    delay(current.duration())
    visible.targetState = false
  }
  // 监听动画结束
  LaunchedEffect(visible.isIdle) {
    // 动画结束且不可见
    if (visible.isIdle && visible.currentState.not() && visible.targetState == visible.currentState) {
      current?.onDismiss()
    }
  }

  current?.let {
    AnimatedVisibility(visibleState = visible, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
      toast(it)
    }
  }
}

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

data class ToastData(val message: String, private val continuation: CancellableContinuation<Unit>) {
  fun duration(): Long = if (message.length <= 20) 2000 else 3500

  fun onDismiss() {
    if (continuation.isActive) continuation.resume(Unit)
  }
}

@Stable
class ToastState {
  private val mutex = Mutex()

  var currentToast by mutableStateOf<ToastData?>(null)
    private set

  suspend fun showToast(message: String) {
    mutex.withLock {
      try {
        return suspendCancellableCoroutine { continuation ->
          currentToast = ToastData(message, continuation)
        }
      } finally {
        currentToast = null
      }
    }
  }
}