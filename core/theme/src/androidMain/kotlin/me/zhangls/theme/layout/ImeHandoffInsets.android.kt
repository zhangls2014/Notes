package me.zhangls.theme.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val KeyboardHandoffDelayMillis = 200L
private const val KeyboardHandoffWindowMillis = 400L

@Composable
actual fun rememberImeHandoffInsets(focusState: ImeHandoffFocusState): WindowInsets {
  val ime = WindowInsets.ime
  val density = LocalDensity.current
  var expiredGeneration by remember(ime, density, focusState) {
    mutableIntStateOf(focusState.handoffGeneration)
  }
  var retainedBottom by remember(ime, density) { mutableIntStateOf(ime.getBottom(density)) }
  val protectHandoff = remember(ime, density, focusState) {
    { focusState.focusedInput != null && focusState.handoffGeneration != expiredGeneration }
  }

  LaunchedEffect(ime, density, focusState, focusState.handoffGeneration) {
    val generation = focusState.handoffGeneration
    // Expire even if the replacement keyboard has exactly the same height.
    delay(KeyboardHandoffWindowMillis)
    expiredGeneration = generation
  }

  LaunchedEffect(ime, density, focusState) {
    snapshotFlow { Triple(ime.getBottom(density), protectHandoff(), focusState.handoffGeneration) }
      .collectLatest { (bottom, protecting, _) ->
        // Only focus handoffs debounce decreases. Normal dismissal follows the real inset.
        // Each new inset cancels the pending release; protection has a bounded lifetime.
        if (protecting && bottom < retainedBottom) delay(KeyboardHandoffDelayMillis)
        retainedBottom = bottom
      }
  }

  return remember(ime, density, focusState) {
    object : WindowInsets by ime {
      override fun getBottom(density: Density): Int =
        // Read during measurement: taller keyboards and normal dismissal never wait for a flow.
        maxOf(ime.getBottom(density), if (protectHandoff()) retainedBottom else 0)
    }
  }
}
