package me.zhangls.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import me.zhangls.login.api.LoginDestination
import me.zhangls.main.api.FavoritesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.SettingsDestination

/** 当前根只保存选择，不包含 Tab 访问历史；每个根分别拥有页面栈。 */
internal class AppNavigationState(
  val selection: NavBackStack<NavKey>,
  val backStacks: Map<NavKey, NavBackStack<NavKey>>,
) {
  val selectedRoot: NavKey get() = selection.single()
  val backStack: NavBackStack<NavKey> get() = backStacks.getValue(selectedRoot)
}

internal val navigationRoots: List<NavKey> =
  listOf(HomeDestination, FavoritesDestination, SettingsDestination, LoginDestination())

internal fun NavKey.stackRoot(): NavKey = if (this is LoginDestination) LoginDestination() else this

@Composable
internal fun rememberAppNavigationState(
  configuration: SavedStateConfiguration,
  initialStack: List<NavKey>,
): AppNavigationState {
  val initialRoot = initialStack.first().stackRoot()
  val selection = rememberNavBackStack(configuration, initialRoot)
  val stacks = navigationRoots.associateWith { root ->
    key(root) {
      rememberNavBackStack(
        configuration,
        *(if (root == initialRoot) initialStack else emptyList()).toTypedArray(),
      )
    }
  }
  return remember { AppNavigationState(selection, stacks) }
}
