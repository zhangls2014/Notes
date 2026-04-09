package me.zhangls.login

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic


val loginNavModule = SerializersModule {
  polymorphic(NavKey::class) {
    subclass(LoginDestination::class, LoginDestination.serializer())
  }
}

fun EntryProviderScope<NavKey>.loginNavEntry(onLoginResult: (LoginResult) -> Unit) {
  entry<LoginDestination> {
    LoginScreen(onLoginResult = onLoginResult)
  }
}
