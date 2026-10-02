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

/** Sequential fades keep a single active NavDisplay and its original Entry state ownership. */
@Composable
internal fun TabFadeThrough(
  selected: TabDestination?,
  onSelectTab: (TabDestination) -> Unit,
  content: @Composable (alpha: () -> Float, selected: TabDestination?, onSelect: (TabDestination) -> Unit) -> Unit,
) {
  if (selected == null) {
    content({ 1f }, null, onSelectTab)
    return
  }
  val alpha = remember { Animatable(1f) }
  val readAlpha = remember(alpha) { { alpha.value } }
  val scope = rememberCoroutineScope()
  var requested by remember { mutableStateOf<TabDestination?>(null) }
  var job by remember { mutableStateOf<Job?>(null) }
  content(readAlpha, requested ?: selected) { target ->
    if (target == requested || (target == selected && job?.isActive != true)) return@content
    job?.cancel()
    requested = target
    job = scope.launch {
      if (target != selected) {
        alpha.animateTo(0f, tween(90, easing = LinearEasing))
        onSelectTab(target)
      }
      alpha.animateTo(1f, tween(210, easing = LinearEasing))
      requested = null
    }
  }
}
