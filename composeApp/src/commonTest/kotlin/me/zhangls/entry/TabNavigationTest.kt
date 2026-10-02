package me.zhangls.entry

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.api.EmailListScene
import me.zhangls.framework.nav.NavEffect
import me.zhangls.login.api.LoginDestination
import me.zhangls.main.api.FavoritesDestination
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.SettingsDestination
import me.zhangls.profile.api.ProfileDestination
import me.zhangls.profile.api.ProfileOrigin
import kotlin.test.Test
import kotlin.test.assertEquals

class TabNavigationTest {
  private fun navigationState(initial: NavBackStack<NavKey>): AppNavigationState {
    val root = initial.first().stackRoot()
    return AppNavigationState(
      NavBackStack(root),
      navigationRoots.associateWith { if (it == root) initial else NavBackStack<NavKey>() },
    )
  }

  @Test
  fun profilePreservesOriginThroughLoginGuardAndReturn() {
    for ((origin, root) in listOf(ProfileOrigin.Home to HomeDestination, ProfileOrigin.Settings to SettingsDestination)) {
      val profile = ProfileDestination(origin)
      val state = navigationState(NavBackStack<NavKey>(root))
      val handler = NavHandler(state) { false }
      handler(NavEffect.Navigate(profile))
      assertEquals(listOf<NavKey>(LoginDestination(redirectTo = profile)), state.backStack.toList())
      handler(NavEffect.Replace(profile), isLogin = true)
      assertEquals(listOf<NavKey>(root, profile), state.backStack.toList())
      handler(NavEffect.Popup)
      assertEquals(listOf<NavKey>(root), state.backStack.toList())
    }
  }

  @Test
  fun explicitTabNavigationOpensItsRootEvenFromItsOwnDetail() {
    val stack = NavBackStack<NavKey>(HomeDestination, EmailDetailDestination(7L))
    val state = navigationState(stack)
    val handler = NavHandler(state) { true }
    handler(NavEffect.Navigate(HomeDestination))
    assertEquals(listOf<NavKey>(HomeDestination), state.backStack.toList())
  }

  @Test
  fun switchingTabsRestoresHistoryAndReselectingKeepsDetail() {
    val stack = NavBackStack<NavKey>(HomeDestination)
    val state = navigationState(stack)
    val handler = NavHandler(state) { true }
    handler.selectTab(FavoritesDestination)
    val detail = EmailDetailDestination(7L, EmailListScene.Favorites)
    handler(NavEffect.Navigate(detail))
    handler.selectTab(FavoritesDestination)
    assertEquals(listOf<NavKey>(FavoritesDestination, detail), state.backStack.toList())
    handler.selectTab(SettingsDestination)
    handler(NavEffect.Popup)
    assertEquals(listOf<NavKey>(SettingsDestination), state.backStack.toList())
    handler.selectTab(FavoritesDestination)
    assertEquals(listOf<NavKey>(FavoritesDestination, detail), state.backStack.toList())
  }

  @Test
  fun runtimeDeepLinkSelectsTheOwningList() {
    val stack = NavBackStack<NavKey>(SettingsDestination)
    val state = navigationState(stack)
    val handler = NavHandler(state) { true }
    val detail = EmailDetailDestination(7L, EmailListScene.Favorites)
    handler(NavEffect.Navigate(detail))
    assertEquals(listOf<NavKey>(FavoritesDestination, detail), state.backStack.toList())
    handler(NavEffect.Navigate(HomeDestination))
    assertEquals(listOf<NavKey>(HomeDestination), state.backStack.toList())
  }

  @Test
  fun logoutAndUnauthenticatedSelectionCannotRetainProtectedPages() {
    val stack = NavBackStack<NavKey>(FavoritesDestination, EmailDetailDestination(7L, EmailListScene.Favorites))
    var loggedIn = true
    val state = navigationState(stack)
    val handler = NavHandler(state) { loggedIn }
    handler.selectTab(HomeDestination)
    handler(NavEffect.Navigate(EmailDetailDestination(8L)))
    handler.selectTab(SettingsDestination)
    handler(NavEffect.Restart(LoginDestination()))
    assertEquals(listOf<NavKey>(LoginDestination()), state.backStack.toList())
    assertEquals(true, state.backStacks.filterKeys { it !is LoginDestination }.values.all { it.isEmpty() })
    loggedIn = false
    handler.selectTab(SettingsDestination)
    assertEquals(listOf<NavKey>(LoginDestination(redirectTo = SettingsDestination)), state.backStack.toList())
  }

  @Test
  fun tabRootsDoNotContainHomeHistory() {
    for (tab in listOf(HomeDestination, FavoritesDestination, SettingsDestination)) {
      assertEquals(listOf<NavKey>(tab), initialBackStack(tab, isLogin = true))
      val stack = NavBackStack<NavKey>(HomeDestination)
      val state = navigationState(stack)
      val handler = NavHandler(state) { true }
      handler(NavEffect.Restart(tab))
      handler(NavEffect.Popup)
      assertEquals(listOf<NavKey>(tab), state.backStack.toList())
    }
  }

  @Test
  fun favoritesDeepLinkReturnsToFavorites() {
    val detail = EmailDetailDestination(42L, EmailListScene.Favorites)
    assertEquals(listOf<NavKey>(FavoritesDestination, detail), initialBackStack(detail, true))
  }

  @Test
  fun loginRedirectRebuildsTheOwningTab() {
    val detail = EmailDetailDestination(42L, EmailListScene.Favorites)
    val initial = initialBackStack(detail, false)
    assertEquals(listOf<NavKey>(LoginDestination(redirectTo = detail)), initial)
    val stack = NavBackStack<NavKey>(*initial.toTypedArray())
    val state = navigationState(stack)
    val handler = NavHandler(state) { false }
    handler(NavEffect.Replace(detail), isLogin = true)
    assertEquals(emptyList(), state.backStacks.getValue(LoginDestination()).toList())
    assertEquals(listOf<NavKey>(FavoritesDestination, detail), state.backStack.toList())
    handler(NavEffect.Popup)
    assertEquals(listOf<NavKey>(FavoritesDestination), state.backStack.toList())
  }

  @Test
  fun detailNavigationDeduplicatesAndReturnsToItsList() {
    val stack = NavBackStack<NavKey>(FavoritesDestination)
    val state = navigationState(stack)
    val handler = NavHandler(state) { true }
    val detail = EmailDetailDestination(7L, EmailListScene.Favorites)
    repeat(2) { handler(NavEffect.Navigate(detail)) }
    assertEquals(listOf<NavKey>(FavoritesDestination, detail), state.backStack.toList())
    handler(NavEffect.Popup)
    assertEquals(listOf<NavKey>(FavoritesDestination), state.backStack.toList())
  }
}
