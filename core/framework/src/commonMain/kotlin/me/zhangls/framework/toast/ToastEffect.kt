package me.zhangls.framework.toast

import me.zhangls.framework.mvi.MviEffect
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Clock


/**
 * @author zhangls
 */
data class ToastEffect(
  val resId: StringResource,
  val timestamp: Long = Clock.System.now().toEpochMilliseconds()
) : MviEffect