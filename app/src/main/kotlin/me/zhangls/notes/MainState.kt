package me.zhangls.notes

import kotlinx.serialization.Serializable
import me.zhangls.framework.mvi.MviState


/**
 * @author zhangls
 */
@Serializable
data class MainState(val isLogin: Boolean?) : MviState
