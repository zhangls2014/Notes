package me.zhangls.about.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import notes.feature.about.generated.resources.Res
import notes.feature.about.generated.resources.about_cache_clear_confirm
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private enum class CacheRowPosition { Closed, Open }

@Composable
internal fun CacheFileSwipeActions(
  isActive: Boolean,
  onSwipe: () -> Unit,
  label: String,
  onDelete: () -> Unit,
  content: @Composable () -> Unit,
) {
  val actionWidth = 88.dp
  val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }
  val state = rememberCacheSwipeState(actionWidthPx, isActive)
  val interactions = remember { MutableInteractionSource() }
  ObserveCacheSwipe(state, interactions, isActive, onSwipe)
  val scope = rememberCoroutineScope()
  val colors = MaterialTheme.colorScheme
  val requestDelete = {
    scope.launch { state.animateTo(CacheRowPosition.Closed) }
    onDelete()
  }
  Box(
    Modifier
      .anchoredDraggable(
        state,
        reverseDirection = false,
        orientation = Orientation.Horizontal,
        interactionSource = interactions,
      )
      .semantics {
        customActions = listOf(
          CustomAccessibilityAction(label) {
            requestDelete()
            true
          },
        )
      },
  ) {
    Box(
      Modifier.matchParentSize().background(colors.errorContainer),
      contentAlignment = AbsoluteAlignment.CenterRight,
    ) {
      if (state.requireOffset() < 0f) {
        Box(
          Modifier.width(actionWidth).fillMaxHeight()
            .clickable(role = Role.Button, onClick = requestDelete)
            .semantics { contentDescription = label },
          contentAlignment = Alignment.Center,
        ) {
          Text(stringResource(Res.string.about_cache_clear_confirm), color = colors.onErrorContainer)
        }
      }
    }
    Box(
      Modifier.absoluteOffset { IntOffset(state.requireOffset().roundToInt(), 0) }.background(colors.surfaceContainer),
    ) {
      content()
    }
  }
}

@Composable
private fun ObserveCacheSwipe(
  state: AnchoredDraggableState<CacheRowPosition>,
  interactions: MutableInteractionSource,
  isActive: Boolean,
  onSwipe: () -> Unit,
) {
  val notifySwipe by rememberUpdatedState(onSwipe)
  LaunchedEffect(interactions) {
    interactions.interactions.collect { interaction ->
      if (interaction is DragInteraction.Start) notifySwipe()
    }
  }
  LaunchedEffect(isActive) {
    if (!isActive) state.animateTo(CacheRowPosition.Closed)
  }
}

@Composable
private fun rememberCacheSwipeState(
  actionWidthPx: Float,
  isActive: Boolean,
): AnchoredDraggableState<CacheRowPosition> {
  val anchors = remember(actionWidthPx) {
    DraggableAnchors {
      CacheRowPosition.Closed at 0f
      CacheRowPosition.Open at -actionWidthPx
    }
  }
  return rememberSaveable(
    actionWidthPx,
    saver = Saver(
      save = { state: AnchoredDraggableState<CacheRowPosition> -> state.targetValue.name },
      restore = { saved ->
        val position = if (isActive) CacheRowPosition.valueOf(saved) else CacheRowPosition.Closed
        AnchoredDraggableState(position, anchors)
      },
    ),
  ) {
    AnchoredDraggableState(CacheRowPosition.Closed, anchors)
  }
}
