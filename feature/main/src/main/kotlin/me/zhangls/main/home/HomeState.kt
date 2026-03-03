package me.zhangls.main.home

import kotlinx.serialization.Serializable
import me.zhangls.framework.mvi.MviState

/**
 * @author zhangls
 */
@Serializable
data class HomeState(val greeting: String) : MviState
