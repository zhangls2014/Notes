package me.zhangls.notes.ui.home

import kotlinx.serialization.Serializable
import me.zhangls.framework.nav.Destination
import me.zhangls.framework.nav.RequireLogin

/**
 * 返回堆栈的键，每个页面都需要定义一个对应的键。
 *
 * @author zhangls
 */
@Serializable
data object HomeDestination : Destination, RequireLogin