package me.zhangls.entry

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import me.zhangls.about.api.AboutDestination
import me.zhangls.about.api.OpenSourceNoticesDestination
import me.zhangls.about.api.OpenSourcePage
import me.zhangls.about.api.ReleaseHistoryDestination
import me.zhangls.framework.nav.Destination
import me.zhangls.framework.nav.NavEffect
import me.zhangls.main.api.HomeDestination
import me.zhangls.main.api.SettingsDestination
import kotlin.test.Test
import kotlin.test.assertEquals

class OpenSourceNavigationTest {
  @Test
  fun historyReturnsToAboutAndUsesSettingsRootWithoutLogin() {
    val state = AppNavigationState(
      NavBackStack(SettingsDestination),
      navigationRoots.associateWith { NavBackStack<NavKey>(it) },
    )
    val handler = NavHandler(state) { false }
    handler(NavEffect.Navigate(AboutDestination))
    handler(NavEffect.Navigate(ReleaseHistoryDestination))
    assertEquals(listOf<NavKey>(SettingsDestination, AboutDestination, ReleaseHistoryDestination), state.backStack.toList())
    handler(NavEffect.Popup)
    assertEquals(AboutDestination, state.backStack.last())
  }

  @Test
  fun noticesAndLicensesReturnThroughAboutToSettings() {
    val settings = NavBackStack<NavKey>(SettingsDestination)
    val home = NavBackStack<NavKey>(HomeDestination)
    val state = AppNavigationState(
      NavBackStack(SettingsDestination),
      navigationRoots.associateWith { root ->
        when (root) {
          SettingsDestination -> settings
          HomeDestination -> home
          else -> NavBackStack<NavKey>()
        }
      },
    )
    val handler = NavHandler(state) { false }
    val notices = OpenSourceNoticesDestination()
    val licenses = OpenSourceNoticesDestination(OpenSourcePage.LICENSES, "license-apache-2-0")
    val expected = listOf<Destination>(SettingsDestination, AboutDestination, notices, licenses)
    expected.drop(1).forEach { handler(NavEffect.Navigate(it)) }
    assertEquals(SettingsDestination, state.selectedRoot)
    assertEquals(expected, state.backStack.toList())
    for (size in 3 downTo 1) {
      handler(NavEffect.Popup)
      assertEquals(SettingsDestination, state.selectedRoot)
      assertEquals(expected.take(size), state.backStack.toList())
    }
    assertEquals(listOf<NavKey>(HomeDestination), home.toList())
  }

  @Test
  fun choosingAnotherDetailReplacesItAndReturnsToList() {
    val state = AppNavigationState(
      NavBackStack(SettingsDestination),
      navigationRoots.associateWith { NavBackStack<NavKey>(it) },
    )
    val handler = NavHandler(state) { true }
    val list = OpenSourceNoticesDestination()
    handler(NavEffect.Navigate(list))
    handler(NavEffect.Navigate(OpenSourceNoticesDestination(OpenSourcePage.LICENSES, "license-MIT")))
    val project = OpenSourceNoticesDestination(projectUrl = "https://example.com")
    handler(NavEffect.Navigate(project))
    assertEquals(listOf<NavKey>(SettingsDestination, list, project), state.backStack.toList())
    handler(NavEffect.Popup)
    assertEquals(list, state.backStack.last())
  }

  @Test
  fun eitherPageOpenedDirectlyKeepsSettingsAsItsRoot() {
    for (page in OpenSourcePage.entries) {
      val destination = OpenSourceNoticesDestination(page)
      val state = AppNavigationState(
        NavBackStack(SettingsDestination),
        navigationRoots.associateWith { NavBackStack<NavKey>(it) },
      )
      val handler = NavHandler(state) { true }
      handler(NavEffect.Navigate(destination))
      assertEquals(listOf<NavKey>(SettingsDestination, destination), state.backStack.toList())
    }
  }
}
