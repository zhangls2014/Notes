package me.zhangls.main

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import me.zhangls.framework.nav.registerDestination
import me.zhangls.main.api.MainEntry
import me.zhangls.main.api.MainResult
import org.koin.compose.koinInject


val mainNavModule = SerializersModule {
  registerDestination(MainDestination::class, MainDestination.serializer())
}

fun EntryProviderScope<NavKey>.mainNavEntry(onResult: (MainResult) -> Unit) {
  entry<MainDestination> {
    koinInject<MainEntry>().Screen(onResult = onResult)
  }
}
