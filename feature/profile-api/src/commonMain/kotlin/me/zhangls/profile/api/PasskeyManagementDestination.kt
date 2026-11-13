package me.zhangls.profile.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.zhangls.framework.nav.Destination
import me.zhangls.framework.nav.RequireLogin

@Serializable
@SerialName("me.zhangls.main.api.PasskeyManagementDestination")
data object PasskeyManagementDestination : Destination, RequireLogin
