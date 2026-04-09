package me.zhangls.entry

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.plus
import me.zhangls.email.detail.EmailDetailDestination
import me.zhangls.email.emailNavEntry
import me.zhangls.email.emailNavModule
import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.nav.Destination
import me.zhangls.framework.nav.NavEffect
import me.zhangls.framework.nav.NavEffect.Restart
import me.zhangls.framework.nav.RequireLogin
import me.zhangls.login.LoginDestination
import me.zhangls.login.LoginResult
import me.zhangls.login.loginNavEntry
import me.zhangls.login.loginNavModule
import me.zhangls.main.MainDestination
import me.zhangls.main.MainResult
import me.zhangls.main.mainNavEntry
import me.zhangls.main.mainNavModule

/**
 * @author zhangls
 */
@Composable
fun AppNavHost(
  viewmodel: MainViewModel,
  deepLinkDestination: DeepLinkDestination? = null,
  onDeepLinkConsumed: () -> Unit = {},
) {
  // 待处理的目标页面
  var pendingDestination by remember { mutableStateOf<Destination?>(null) }
  //
  val config = SavedStateConfiguration {
    serializersModule = mainNavModule + loginNavModule + emailNavModule
  }
  // 返回堆栈
  val backStack = rememberNavBackStack(config)
  // 登录状态
  val state by viewmodel.state.collectAsStateWithLifecycle()
  // 是否登录
  var isLogin by remember { mutableStateOf(false) }

  val navHandler = NavHandler(backStack = backStack, isLogin = { isLogin }, onIntercept = { pendingDestination = it })

  if (state.isLogin != null) {
    isLogin = state.isLogin ?: false

    // 回退栈为空，则根据登录状态添加首屏
    if (backStack.isEmpty()) {
      val firstDest = deepLinkDestination ?: if (isLogin) MainDestination else LoginDestination
      navHandler(NavEffect.Navigate(firstDest))

      if (deepLinkDestination != null) onDeepLinkConsumed()
    } else if (deepLinkDestination != null) {
      navHandler(NavEffect.Navigate(deepLinkDestination))
      onDeepLinkConsumed()
    }
  } else {
    // 登录状态未知，不显示 UI
    return
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
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
      mainNavEntry { result ->
        when (result) {
          MainResult.Logout -> {
            navHandler(Restart(LoginDestination))
          }

          is MainResult.NavigateToEmailDetail -> {
            navHandler(
              NavEffect.Navigate(EmailDetailDestination(result.emailId)),
            )
          }
        }
      }

      loginNavEntry {
        if (it == LoginResult.Success) {
          pendingDestination?.let { dest ->
            pendingDestination = null
            // 登录成功，且跳转目标页面不为空，则跳转到目标页面
            navHandler(NavEffect.Replace(dest), isLogin = true)
          } ?: run {
            // 登录成功，且跳转目标页面为空，则跳转到主页
            navHandler(NavEffect.Replace(MainDestination), isLogin = true)
          }
        }
      }

      emailNavEntry {
        navHandler(it)
      }
    }
  )
}


private class NavHandler(
  private val backStack: NavBackStack<NavKey>,
  private val isLogin: () -> Boolean,
  private val onIntercept: (Destination) -> Unit
) {
  operator fun invoke(effect: NavEffect, isLogin: Boolean? = null) {
    backStack.handle(effect, isLogin ?: this.isLogin(), onIntercept)
  }

  fun NavBackStack<NavKey>.handle(
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

      is Restart -> {
        clear()
        navCheck(target = effect.dest, isLogin = isLogin, onIntercept = onIntercept)
      }

      is NavEffect.Popup -> {
        if (size > 1) {
          removeLastOrNull()
        } else {
          val target = if (isLogin) MainDestination else LoginDestination
          if (firstOrNull() != target) {
            add(0, target)
            removeLastOrNull()
          }
        }
      }
    }
  }

  fun NavBackStack<NavKey>.navCheck(
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
}
