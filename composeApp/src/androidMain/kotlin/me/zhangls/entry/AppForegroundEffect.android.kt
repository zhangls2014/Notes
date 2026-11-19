package me.zhangls.entry

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberConfigurationChangeCheck(): () -> Boolean {
  val activity = LocalActivity.current
  return remember(activity) { { activity?.isChangingConfigurations == true } }
}
