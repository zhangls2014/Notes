package me.zhangls.about.api

import kotlinx.serialization.Serializable
import me.zhangls.framework.nav.Destination

/** Public application information, available without authentication. */
@Serializable
data object AboutDestination : Destination
