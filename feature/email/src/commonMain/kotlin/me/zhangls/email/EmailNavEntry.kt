package me.zhangls.email

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.api.EmailEntry
import me.zhangls.framework.nav.NavEffect
import org.koin.compose.koinInject

/**
 * 邮件详情页的导航条目。
 *
 * key 与它的序列化登记都在契约模块（[me.zhangls.email.api.EmailNavigation]）——
 * 那里是"这个 feature 有哪些导航目的地"的唯一出处，本文件只负责把 key 与内容装配起来。
 */
fun EntryProviderScope<NavKey>.emailNavEntry(effect: (NavEffect) -> Unit) {
  entry<EmailDetailDestination> {
    koinInject<EmailEntry>().DetailScreen(
      emailId = it.emailId,
      onBackPressed = { effect(NavEffect.Popup) },
    )
  }
}
