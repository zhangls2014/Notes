package me.zhangls.profile.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.zhangls.framework.nav.Destination
import me.zhangls.framework.nav.RequireLogin

/** 发起入口随导航 key 保存，使返回和登录恢复回到原来的 Tab。 */
@Serializable
enum class ProfileOrigin { Home, Settings }

/** 当前登录用户的个人信息；没有列表—详情窗格标注。 */
// 移动模块时保留旧的 wire identity，兼容已保存的 NavKey / 登录 redirectTo。
@Serializable
@SerialName("me.zhangls.settings.api.ProfileDestination")
data class ProfileDestination(val origin: ProfileOrigin = ProfileOrigin.Settings) : Destination, RequireLogin
