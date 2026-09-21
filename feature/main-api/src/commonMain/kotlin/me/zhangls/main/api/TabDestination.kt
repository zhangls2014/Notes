package me.zhangls.main.api

import kotlinx.serialization.Serializable
import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.nav.RequireLogin

/**
 * 主界面的一个 Tab，同时是导航返回栈里的目的地。
 *
 * Tab 之所以必须是**独立的目的地**，而不是同一个目的地内部的分页，有两层原因：
 * 1. 只有独立目的地才各自拥有 ViewModel 作用域 —— 同一个 `NavEntry` 内的多个"屏幕"共用一个
 *    `ViewModelStore`，只能靠 `koinViewModel(key = ...)` 手工区分；
 * 2. 应用外壳（`NavigationSuiteScaffold`）必须待在 `NavDisplay` **之外**，而它判断"当前选中
 *    哪个 Tab"的依据就是返回栈的根 —— 分页方案里三个 Tab 同属一个 key，外壳无从判断。
 *
 * 每个 Tab 一个独立类型而不是一个枚举的三个常量：导航条目按 key 的**类型**匹配，
 * 枚举常量共享同一个类型，`entry<TabDestination>` 无法把三个 Tab 分开。
 */
@Serializable
sealed interface TabDestination : DeepLinkDestination, RequireLogin

@Serializable
data object HomeDestination : TabDestination

@Serializable
data object FavoritesDestination : TabDestination

@Serializable
data object SettingsDestination : TabDestination
