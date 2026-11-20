package me.zhangls.theme.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Shared appearance for account fields; editing state belongs to the caller. */
@Composable
fun AccountTextField(
  state: TextFieldState,
  label: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isError: Boolean = false,
  leadingIcon: (@Composable () -> Unit)? = null,
  trailingIcon: (@Composable () -> Unit)? = null,
  supportingText: (@Composable () -> Unit)? = null,
  inputTransformation: InputTransformation? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  onKeyboardAction: KeyboardActionHandler? = null,
) {
  OutlinedTextField(
    state = state,
    label = { label() },
    modifier = modifier,
    enabled = enabled,
    isError = isError,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    supportingText = supportingText,
    inputTransformation = inputTransformation,
    keyboardOptions = keyboardOptions,
    onKeyboardAction = onKeyboardAction,
    textStyle = MaterialTheme.typography.bodyLarge,
    shape = MaterialTheme.shapes.large,
    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
    lineLimits = TextFieldLineLimits.SingleLine,
  )
}

@Composable
fun AccountSecureTextField(
  state: TextFieldState,
  label: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isError: Boolean = false,
  leadingIcon: (@Composable () -> Unit)? = null,
  trailingIcon: (@Composable () -> Unit)? = null,
  supportingText: (@Composable () -> Unit)? = null,
  passwordVisible: Boolean = false,
  inputTransformation: InputTransformation? = null,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  onKeyboardAction: KeyboardActionHandler? = null,
) {
  OutlinedSecureTextField(
    state = state,
    label = { label() },
    modifier = modifier,
    enabled = enabled,
    isError = isError,
    leadingIcon = leadingIcon,
    trailingIcon = trailingIcon,
    supportingText = supportingText,
    inputTransformation = inputTransformation,
    keyboardOptions = keyboardOptions,
    onKeyboardAction = onKeyboardAction,
    textStyle = MaterialTheme.typography.bodyLarge,
    shape = MaterialTheme.shapes.large,
    colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
    textObfuscationMode = if (passwordVisible) TextObfuscationMode.Visible else TextObfuscationMode.Hidden,
  )
}
