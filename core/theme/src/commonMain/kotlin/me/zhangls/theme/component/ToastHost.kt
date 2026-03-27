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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.coroutines.resume

@Composable
fun Toast(toastData: ToastData) {
  val message = stringResource(toastData.res)

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
  val current = toastState.currentToast ?: return
  val message = stringResource(current.res)
  val duration = current.duration(message)
  val visible = remember { MutableTransitionState(true) }

  LaunchedEffect(current) {
    visible.targetState = true
    delay(duration)
    visible.targetState = false
  }

  // 监听动画结束
  LaunchedEffect(visible.isIdle) {
    // 动画结束且不可见
    if (visible.isIdle && visible.currentState.not() && visible.targetState == visible.currentState) {
      current.onDismiss()
    }
  }

  AnimatedVisibility(visibleState = visible, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
    toast(current)
  }
}

@Composable
fun rememberToastState(): ToastState = remember { ToastState() }

data class ToastData(val res: StringResource, private val continuation: CancellableContinuation<Unit>) {
  fun duration(message: String): Long = if (message.length <= 20) 2000 else 3500

  fun onDismiss() {
    if (continuation.isActive) continuation.resume(Unit)
  }
}

@Stable
class ToastState {
  private val mutex = Mutex()

  var currentToast by mutableStateOf<ToastData?>(null)
    private set

  suspend fun showToast(res: StringResource) {
    mutex.withLock {
      try {
        return suspendCancellableCoroutine { continuation ->
          currentToast = ToastData(res, continuation)
        }
      } finally {
        currentToast = null
      }
    }
  }
}