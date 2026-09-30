package me.zhangls.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

internal enum class LoginInput { Account, Password }

/** Records the handoff in the focus callback, before the keyboard can send new insets. */
@Stable
internal class LoginInputFocusState {
  var focusedInput by mutableStateOf<LoginInput?>(null)
    private set
  var handoffGeneration by mutableIntStateOf(0)
    private set
  private var lastInput: LoginInput? = null

  fun onFocusChanged(input: LoginInput, focused: Boolean) {
    if (focused) {
      if (lastInput != null && lastInput != input) handoffGeneration++
      lastInput = input
      focusedInput = input
    } else if (focusedInput == input) {
      focusedInput = null
    }
  }
}

/** Platform IME avoidance, including Android's ordinary/security keyboard handoff. */
@Composable
internal expect fun Modifier.loginImePadding(focusState: LoginInputFocusState): Modifier
