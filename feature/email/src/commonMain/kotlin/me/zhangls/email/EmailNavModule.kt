package me.zhangls.email

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import me.zhangls.email.api.EmailEntry
import me.zhangls.email.detail.EmailDetailDestination
import me.zhangls.framework.nav.NavEffect
import org.koin.compose.koinInject


val emailNavModule = SerializersModule {
  polymorphic(NavKey::class) {
    subclass(EmailDetailDestination::class, EmailDetailDestination.serializer())
  }
}

fun EntryProviderScope<NavKey>.emailNavEntry(effect: (NavEffect) -> Unit) {
  entry<EmailDetailDestination> {
    koinInject<EmailEntry>().DetailScreen(
      emailId = it.emailId,
      onBackPressed = { effect(NavEffect.Popup()) },
    )
  }
}
