package me.zhangls.framework.toast

import me.zhangls.framework.mvi.MviEffect
import org.jetbrains.compose.resources.StringResource
import kotlin.time.TimeSource


/**
 * @author zhangls
 */
data class ToastEffect(
  val resId: StringResource,
  val timestamp: Long = TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
) : MviEffect