package me.zhangls.email

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
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
 *
 * 本条目还声明"我是详情栏"（[ListDetailSceneStrategy.detailPane]），并且 `sceneKey`
 * 取自 key 自身携带的 `scene` —— 于是它必定回到**打开它的那个列表**所属的分栏场景里。
 * 少了这一条标注，场景策略会在 `entries.last()` 就返回 null（它从栈顶往回找带窗格标注的条目），
 * 整页详情虽然看起来正常，但分栏永远不会出现：这是实测才会暴露的接线遗漏。
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.emailNavEntry(effect: (NavEffect) -> Unit) {
  entry<EmailDetailDestination>(
    metadata = { destination ->
      ListDetailSceneStrategy.detailPane(sceneKey = destination.scene)
    },
  ) {
    koinInject<EmailEntry>().DetailScreen(
      emailId = it.emailId,
      onBackPressed = { effect(NavEffect.Popup) },
    )
  }
}
