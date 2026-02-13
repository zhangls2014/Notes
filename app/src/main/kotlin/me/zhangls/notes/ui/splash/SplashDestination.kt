package me.zhangls.notes.ui.splash

import kotlinx.serialization.Serializable
import me.zhangls.framework.nav.Destination

/**
 * 返回堆栈的键，每个页面都需要定义一个对应的键。
 *
 * @author zhangls
 */
@Serializable
data object SplashDestination : Destination