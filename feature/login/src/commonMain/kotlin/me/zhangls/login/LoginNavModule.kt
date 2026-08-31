package me.zhangls.login

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import me.zhangls.login.api.LoginEntry
import me.zhangls.login.api.LoginResult
import org.koin.compose.koinInject


val loginNavModule = SerializersModule {
  polymorphic(NavKey::class) {
    subclass(LoginDestination::class, LoginDestination.serializer())
  }
}

fun EntryProviderScope<NavKey>.loginNavEntry(onLoginResult: (LoginResult) -> Unit) {
  entry<LoginDestination> {
    koinInject<LoginEntry>().Screen(onLoginResult = onLoginResult)
  }
}
