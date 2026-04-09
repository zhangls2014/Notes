package me.zhangls.main

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic


val mainNavModule = SerializersModule {
  polymorphic(NavKey::class) {
    subclass(MainDestination::class, MainDestination.serializer())
  }
}

fun EntryProviderScope<NavKey>.mainNavEntry(onResult: (MainResult) -> Unit) {
  entry<MainDestination> {
    MainScreen(onResult = onResult)
  }
}
