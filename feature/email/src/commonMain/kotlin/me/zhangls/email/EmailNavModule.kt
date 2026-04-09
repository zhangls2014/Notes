package me.zhangls.email

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import me.zhangls.email.detail.EmailDetailDestination
import me.zhangls.email.detail.EmailDetailScreen
import me.zhangls.framework.nav.NavEffect


val emailNavModule = SerializersModule {
  polymorphic(NavKey::class) {
    subclass(EmailDetailDestination::class, EmailDetailDestination.serializer())
  }
}

fun EntryProviderScope<NavKey>.emailNavEntry(effect: (NavEffect) -> Unit) {
  entry<EmailDetailDestination> {
    EmailDetailScreen(emailId = it.emailId, onBackPressed = { effect(NavEffect.Popup()) })
  }
}
