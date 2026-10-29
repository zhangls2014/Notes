package me.zhangls.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.api.EmailListScene
import me.zhangls.main.api.HomeDestination

/** The host must retain this marker while a configuration change recreates its composition. */
@Composable
internal fun rememberHomeDraftVisibility(): MutableState<Boolean> = rememberSaveable { mutableStateOf(false) }

/** Keep the list that owns the draft composed when the detail would occupy the narrow window. */
@Composable
internal fun KeepHomeDraftVisible(backStack: NavBackStack<NavKey>, singlePane: Boolean, draftVisible: Boolean) {
  val detail = backStack.lastOrNull()
  LaunchedEffect(backStack, singlePane, draftVisible, detail) {
    if (singlePane && draftVisible && backStack.firstOrNull() == HomeDestination) {
      while ((backStack.lastOrNull() as? EmailDetailDestination)?.scene == EmailListScene.Home) {
        backStack.removeLastOrNull()
      }
    }
  }
}
