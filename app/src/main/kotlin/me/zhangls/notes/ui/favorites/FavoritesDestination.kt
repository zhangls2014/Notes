package me.zhangls.notes.ui.favorites

import kotlinx.serialization.Serializable
import me.zhangls.framework.deeplink.DeepLinkDestination
import me.zhangls.framework.nav.RequireLogin

/**
 * 返回堆栈的键，每个页面都需要定义一个对应的键。
 *
 * @author zhangls
 */
@Serializable
data object FavoritesDestination : DeepLinkDestination, RequireLogin