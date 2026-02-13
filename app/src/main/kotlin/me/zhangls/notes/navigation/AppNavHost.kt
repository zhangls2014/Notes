package me.zhangls.notes.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import me.zhangls.framework.nav.Destination
import me.zhangls.framework.nav.NavEffect
import me.zhangls.framework.nav.RequireLogin
import me.zhangls.login.LoginDestination
import me.zhangls.login.LoginResult
import me.zhangls.login.LoginScreen
import me.zhangls.notes.ui.detail.DetailDestination
import me.zhangls.notes.ui.detail.DetailScreen
import me.zhangls.notes.ui.home.HomeDestination
import me.zhangls.notes.ui.home.HomeResult
import me.zhangls.notes.ui.home.HomeScreen
import me.zhangls.notes.ui.splash.SplashDestination
import me.zhangls.notes.ui.splash.SplashScreen

/**
 * @author zhangls
 */
@Composable
fun AppNavHost(viewmodel: MainViewModel = hiltViewModel()) {
  // 返回堆栈
  val backStack = rememberNavBackStack(SplashDestination)
  // 待处理的目标页面
  var pendingDestination by remember { mutableStateOf<Destination?>(null) }
  // 登录状态
  val state by viewmodel.state.collectAsState()

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
      entry<SplashDestination> {
        SplashScreen {
          val dest = if (state.isLogin) HomeDestination else LoginDestination
          backStack.handle(
            effect = NavEffect.Replace(dest),
            isLogin = state.isLogin,
            onIntercept = { pendingDestination = it }
          )
        }
      }

      entry<HomeDestination> {
        HomeScreen {
          when (it) {
            is HomeResult.Detail -> {
              backStack.handle(
                effect = NavEffect.Navigate(DetailDestination(it.id)),
                isLogin = state.isLogin,
                onIntercept = { pendingDestination = it }
              )
            }

            HomeResult.Logout -> {
              backStack.handle(
                effect = NavEffect.Restart(LoginDestination),
                isLogin = state.isLogin,
                onIntercept = { pendingDestination = it }
              )
            }
          }
        }
      }

      entry<LoginDestination> {
        LoginScreen {
          if (it == LoginResult.Success) {
            pendingDestination?.let { dest ->
              pendingDestination = null
              backStack.handle(NavEffect.Replace(dest), isLogin = true)
            } ?: run {
              backStack.handle(NavEffect.Popup())
            }
          }
        }
      }

      entry<DetailDestination> { dest ->
        DetailScreen(dest) {
          backStack.handle(
            effect = it,
            isLogin = state.isLogin,
            onIntercept = { pendingDestination = it }
          )
        }
      }
    }
  )
}

private fun NavBackStack<NavKey>.handle(
  effect: NavEffect,
  isLogin: Boolean = false,
  onIntercept: (Destination) -> Unit = {}
) {
  when (effect) {
    is NavEffect.Navigate -> {
      navCheck(target = effect.dest, isLogin = isLogin, onIntercept = onIntercept)
    }

    is NavEffect.Replace -> {
      removeLastOrNull()
      navCheck(target = effect.dest, isLogin = isLogin, onIntercept = onIntercept)
    }

    is NavEffect.Restart -> {
      clear()
      navCheck(target = effect.dest, isLogin = isLogin, onIntercept = onIntercept)
    }

    is NavEffect.Popup -> {
      if (size > 1) removeLastOrNull()
    }
  }
}

private fun NavBackStack<NavKey>.navCheck(
  target: Destination,
  isLogin: Boolean = false,
  onIntercept: (Destination) -> Unit = {}
) {
  if (target is RequireLogin && !isLogin) {
    onIntercept(target)
    add(LoginDestination)
  } else {
    add(target)
  }
}