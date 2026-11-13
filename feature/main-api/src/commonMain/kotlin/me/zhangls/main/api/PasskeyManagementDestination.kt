package me.zhangls.main.api

import kotlinx.serialization.Serializable
import me.zhangls.framework.nav.Destination
import me.zhangls.framework.nav.RequireLogin

@Serializable
data object PasskeyManagementDestination : Destination, RequireLogin
