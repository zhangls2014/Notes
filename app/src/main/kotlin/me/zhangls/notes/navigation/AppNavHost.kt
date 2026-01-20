package me.zhangls.notes.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import me.zhangls.notes.ui.detail.DetailScreen
import me.zhangls.notes.ui.home.HomeScreen
import me.zhangls.notes.ui.login.LoginScreen

/**
 * @author zhangls
 */
@Composable
fun AppNavHost() {
  // 返回堆栈
  val backStack = rememberNavBackStack(Destination.Login)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators = listOf(
      rememberSaveableStateHolderNavEntryDecorator(),
      rememberViewModelStoreNavEntryDecorator()
    ),
    transitionSpec = {
      slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
    },
    popTransitionSpec = {
      slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
    },
    predictivePopTransitionSpec = {
      slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
    },
    entryProvider = entryProvider {
      entry<Destination.Login> {
        LoginScreen(onNavEffect = { backStack.handle(it) })
      }
      entry<Destination.Home> {
        HomeScreen(onNavEffect = { backStack.handle(it) })
      }
      entry<Destination.Detail> { dest ->
        DetailScreen(
          param = dest,
          onNavEffect = { backStack.handle(it) }
        )
      }
    }
  )
}

private fun NavBackStack<NavKey>.handle(effect: NavEffect) {
  when (effect) {
    is NavEffect.Navigate -> add(effect.dest)
    is NavEffect.Replace -> {
      removeLastOrNull()
      add(effect.dest)
    }

    is NavEffect.Restart -> {
      clear()
      add(effect.dest)
    }

    NavEffect.Back -> {
      if (size > 1) removeLastOrNull()
    }
  }
}