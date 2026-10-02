package me.zhangls.profile

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.zhangls.framework.nav.NavEffect
import me.zhangls.profile.api.ProfileDestination
import me.zhangls.profile.api.ProfileEntry
import org.koin.compose.koinInject

fun EntryProviderScope<NavKey>.profileNavEntry(effect: (NavEffect) -> Unit) {
  entry<ProfileDestination> {
    koinInject<ProfileEntry>().Screen(onBackPressed = { effect(NavEffect.Popup) })
  }
}
