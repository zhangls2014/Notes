package me.zhangls.main.api

import kotlinx.serialization.Serializable
import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.nav.RequireLogin

/**
 * 主界面的一个 Tab，同时是导航返回栈里的目的地。
 *
 * 每个 Tab 是活动返回栈的根，拥有独立 Entry 作用域和列表场景元数据。
 * 切换 Tab 恢复各自的页面栈和 Entry 状态，不把其他 Tab 记录为可返回的页面。
 * 共享外壳位于 NavDisplay 外，从栈根读取选中项。
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
