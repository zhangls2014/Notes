package me.zhangls.framework.mvi


/**
 * @author zhangls
 */
data class ToastEffect(val resId: Int, val timestamp: Long = System.nanoTime()) : MviEffect