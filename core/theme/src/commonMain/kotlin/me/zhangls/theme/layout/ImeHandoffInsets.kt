package me.zhangls.theme.layout

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Records input handoffs in focus callbacks before the keyboard can send new insets. */
@Stable
class ImeHandoffFocusState {
  var focusedInput by mutableStateOf<Any?>(null)
    private set
  var handoffGeneration by mutableIntStateOf(0)
    private set
  private var lastInput: Any? = null

  fun onFocusChanged(input: Any, focused: Boolean) {
    if (focused) {
      if (lastInput != null && lastInput != input) handoffGeneration++
      lastInput = input
      focusedInput = input
    } else if (focusedInput == input) {
      focusedInput = null
    }
  }
}

/** IME insets with temporary Android protection for handoffs between form fields. */
@Composable
expect fun rememberImeHandoffInsets(focusState: ImeHandoffFocusState): WindowInsets
