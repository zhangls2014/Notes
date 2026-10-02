package me.zhangls.about

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.about.api.AboutDestination
import me.zhangls.about.api.AboutEntry
import me.zhangls.framework.nav.NavEffect
import org.koin.compose.koinInject

fun EntryProviderScope<NavKey>.aboutNavEntry(appInfo: AboutAppInfo, effect: (NavEffect) -> Unit) {
  entry<AboutDestination> {
    koinInject<AboutEntry>().Screen(appInfo, onBackPressed = { effect(NavEffect.Popup) })
  }
}
