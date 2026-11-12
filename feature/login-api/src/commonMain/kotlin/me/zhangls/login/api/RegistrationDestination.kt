package me.zhangls.login.api

import kotlinx.serialization.Serializable
import me.zhangls.framework.nav.Destination

@Serializable
data class RegistrationDestination(val redirectTo: Destination? = null) : Destination
