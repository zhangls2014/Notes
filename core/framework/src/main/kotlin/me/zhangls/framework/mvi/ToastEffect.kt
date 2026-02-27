package me.zhangls.framework.mvi

import java.util.UUID


/**
 * @author zhangls
 */
data class ToastEffect(val resId: Int, val uuid: String = UUID.randomUUID().toString()) : MviEffect