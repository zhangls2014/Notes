package me.zhangls.entry

import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.about.api.AboutDestination
import me.zhangls.email.api.EmailListScene
import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.nav.Destination
import me.zhangls.framework.nav.NavEffect
import me.zhangls.framework.nav.RequireLogin
import me.zhangls.login.api.LoginDestination
import me.zhangls.main.api.FavoritesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.TabDestination
import me.zhangls.main.api.SettingsDestination
import me.zhangls.profile.api.ProfileDestination
import me.zhangls.profile.api.ProfileOrigin

/** 首屏与登录恢复都按目标所属的 Tab 建栈，不记录 Tab 访问历史。 */
internal fun initialBackStack(deepLink: DeepLinkDestination?, isLogin: Boolean): List<NavKey> =
  if (!isLogin) {
    listOf(LoginDestination(redirectTo = deepLink?.takeIf { it is RequireLogin }))
  } else {
    stackFor(deepLink ?: HomeDestination)
  }

internal fun List<NavKey>.tabSelection(): TabDestination? = firstOrNull() as? TabDestination

/** 每个 Tab 的返回栈独立保留；顶层选择与子页面导航是两个独立操作。 */
internal class NavHandler(
  private val state: AppNavigationState,
  private val isLogin: () -> Boolean,
) {
  private val backStack: NavBackStack<NavKey> get() = state.backStack

  fun selectTab(tab: TabDestination) {
    val destination = tab.guardedByLogin(isLogin())
    if (destination == backStack.firstOrNull()) return
    Snapshot.withMutableSnapshot {
      if (destination is LoginDestination) reset(destination) else activate(destination)
    }
  }

  operator fun invoke(effect: NavEffect, isLogin: Boolean? = null) {
    val loggedIn = isLogin ?: this.isLogin()
    // 重建期间不向 NavDisplay 暴露空栈或不完整的列表—详情组合。
    Snapshot.withMutableSnapshot {
      when (effect) {
        is NavEffect.Navigate -> push(effect.dest.guardedByLogin(loggedIn))
        is NavEffect.Replace -> {
          backStack.removeLastOrNull()
          push(effect.dest.guardedByLogin(loggedIn))
        }
        is NavEffect.Restart -> reset(effect.dest.guardedByLogin(loggedIn))
        NavEffect.Popup -> if (backStack.size > 1) backStack.removeLastOrNull()
      }
    }
  }

  private fun activate(destination: Destination) {
    val root = rootFor(destination)
    state.selection[0] = root.stackRoot()
    if (backStack.isEmpty()) backStack.add(root)
  }

  private fun reset(destination: Destination) {
    // 会话重置时连非活动 Tab 一起清空，使它们的 Entry 和 ViewModel 被释放。
    state.backStacks.values.forEach { it.clear() }
    activate(destination)
    if (backStack.last() != destination) backStack.add(destination)
  }

  private fun push(destination: Destination) {
    if (destination is LoginDestination) {
      reset(destination)
      return
    }
    val leavingLogin = state.selectedRoot is LoginDestination
    activate(destination)
    if (leavingLogin) state.backStacks.getValue(LoginDestination()).clear()
    // 显式导航到 Tab 根（如深链）回到根；点击导航栏则用 selectTab 恢复原栈。
    if (destination is TabDestination) {
      while (backStack.size > 1) backStack.removeLastOrNull()
    } else if (backStack.lastOrNull() != destination) {
      backStack.add(destination)
    }
  }
}

private fun stackFor(destination: Destination): List<NavKey> {
  val root = rootFor(destination)
  return if (root == destination) listOf(root) else listOf(root, destination)
}

private fun rootFor(destination: Destination): Destination = when (destination) {
  is LoginDestination, is TabDestination -> destination
  is EmailDetailDestination -> when (destination.scene) {
    EmailListScene.Home -> HomeDestination
    EmailListScene.Favorites -> FavoritesDestination
  }
  is ProfileDestination -> when (destination.origin) {
    ProfileOrigin.Home -> HomeDestination
    ProfileOrigin.Settings -> SettingsDestination
  }
  AboutDestination -> SettingsDestination
  else -> HomeDestination
}

private fun Destination.guardedByLogin(isLogin: Boolean): Destination =
  if (this is RequireLogin && !isLogin) LoginDestination(redirectTo = this) else this
