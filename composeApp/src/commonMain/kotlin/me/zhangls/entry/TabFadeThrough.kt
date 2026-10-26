package me.zhangls.entry

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.zhangls.main.api.TabDestination

private const val TAB_FADE_OUT_MILLIS = 90
private const val TAB_FADE_IN_MILLIS = 210

/** Sequential fades keep a single active NavDisplay and its original Entry state ownership. */
@Composable
internal fun TabFadeThrough(
  selected: TabDestination?,
  onSelectTab: (TabDestination) -> Unit,
  content: @Composable (alpha: () -> Float, selected: TabDestination?, onSelect: (TabDestination) -> Unit) -> Unit,
) {
  val alpha = if (selected != null) remember { Animatable(1f) } else null
  val scope = if (selected != null) rememberCoroutineScope() else null
  val readAlpha = remember(alpha) { { alpha?.value ?: 1f } }
  var requested by remember(selected == null) { mutableStateOf<TabDestination?>(null) }
  var job by remember(selected == null) { mutableStateOf<Job?>(null) }
  val selectTab: (TabDestination) -> Unit = if (selected == null) {
    onSelectTab
  } else {
    select@{ target ->
      if (target == requested || (target == selected && job?.isActive != true)) return@select
      job?.cancel()
      requested = target
      val animation = checkNotNull(alpha)
      job = checkNotNull(scope).launch {
        if (target != selected) {
          animation.animateTo(0f, tween(TAB_FADE_OUT_MILLIS, easing = LinearEasing))
          onSelectTab(target)
        }
        animation.animateTo(1f, tween(TAB_FADE_IN_MILLIS, easing = LinearEasing))
        requested = null
      }
    }
  }
  content(readAlpha, requested ?: selected, selectTab)
}
