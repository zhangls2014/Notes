package me.zhangls.notes.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.nav.Destination
import me.zhangls.framework.nav.NavEffect
import me.zhangls.framework.nav.RequireLogin
import me.zhangls.login.LoginDestination
import me.zhangls.login.LoginResult
import me.zhangls.login.LoginScreen
import me.zhangls.main.MainResult
import me.zhangls.main.MainScreen
import me.zhangls.main.favorites.FavoritesDestination
import me.zhangls.main.favorites.FavoritesScreen
import me.zhangls.main.home.HomeDestination

/**
 * @author zhangls
 */
@Composable
fun AppNavHost(viewmodel: MainViewModel, deepLinkDestination: DeepLinkDestination? = null) {
  // 待处理的目标页面
  var pendingDestination by remember { mutableStateOf<Destination?>(null) }
  // 返回堆栈
  val backStack = rememberNavBackStack()
  // 弹窗场景
  val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
  // 登录状态
  val state by viewmodel.state.collectAsState()
  // 是否登录
  var isLogin by remember { mutableStateOf(false) }

  if (state.isLogin != null) {
    isLogin = state.isLogin ?: false

    // 回退栈为空，则根据登录状态添加首屏
    if (backStack.isEmpty()) {
      val firstDest = deepLinkDestination ?: if (isLogin) HomeDestination else LoginDestination
      backStack.handle(
        NavEffect.Navigate(firstDest),
        isLogin = isLogin,
        onIntercept = { pendingDestination = it })
    }
  } else {
    // 登录状态未知，不显示 UI
    return
  }

  NavDisplay(
    backStack = backStack,
    sceneStrategy = dialogStrategy,
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
      entry<HomeDestination> {
        MainScreen { result ->
          when (result) {
            MainResult.Logout -> {
              backStack.handle(effect = NavEffect.Restart(LoginDestination), isLogin = false)
            }
          }
        }
      }

      entry<LoginDestination> {
        LoginScreen {
          if (it == LoginResult.Success) {
            pendingDestination?.let { dest ->
              pendingDestination = null
              // 登录成功，且跳转目标页面不为空，则跳转到目标页面
              backStack.handle(NavEffect.Replace(dest), isLogin = true)
            } ?: run {
              // 登录成功，且跳转目标页面为空，则跳转到主页
              backStack.handle(NavEffect.Replace(HomeDestination), isLogin = true)
            }
          }
        }
      }

      entry<FavoritesDestination> {
        FavoritesScreen()
      }
    }
  )
}

private fun NavBackStack<NavKey>.handle(
  effect: NavEffect,
  isLogin: Boolean,
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
  onIntercept: (Destination) -> Unit
) {
  if (target is RequireLogin && !isLogin) {
    onIntercept(target)
    add(LoginDestination)
  } else {
    add(target)
  }
}