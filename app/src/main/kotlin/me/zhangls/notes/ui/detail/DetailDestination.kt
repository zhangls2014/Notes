package me.zhangls.notes.ui.detail

import kotlinx.serialization.Serializable
import me.zhangls.framework.nav.Destination
import me.zhangls.framework.nav.RequireLogin

/**
 * @author zhangls
 */
@Serializable
data class DetailDestination(val id: Int) : Destination, RequireLogin