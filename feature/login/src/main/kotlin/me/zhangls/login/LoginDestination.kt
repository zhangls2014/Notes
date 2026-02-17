package me.zhangls.login

import kotlinx.serialization.Serializable
import me.zhangls.framework.nav.Destination

/**
 * 登录页面
 *
 * @param afterLogin 登录成功后跳转的页面
 *
 * @author zhangls
 */
@Serializable
data object LoginDestination : Destination