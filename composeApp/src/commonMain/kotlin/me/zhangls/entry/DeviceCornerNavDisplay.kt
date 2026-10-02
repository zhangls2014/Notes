package me.zhangls.entry

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

private const val PredictiveMotionDuration = 220
private val SheetBackEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
private val LocalPredictiveGestureProgress = staticCompositionLocalOf<() -> Float?> { { null } }

/** Single-page entry, return and predictive back share the Material bottom sheet scale curve. */
@Composable
internal fun <T : Any> DeviceCornerNavDisplay(
  entries: List<NavEntry<T>>,
  sceneStrategies: List<SceneStrategy<T>>,
  onBack: () -> Unit,
  corners: DeviceCorners? = rememberDeviceCorners(),
) {
  val strategies = remember(sceneStrategies, corners) {
    sceneStrategies.map { strategy ->
      if (strategy is SinglePaneSceneStrategy<T>) DeviceCornerSceneStrategy(strategy, corners)
      else strategy
    }
  }
  val sceneState = rememberSceneState(entries = entries, sceneStrategies = strategies, onBack = onBack)
  val scene = sceneState.currentScene
  val gestureState = rememberNavigationEventState(
    currentInfo = SceneInfo(scene),
    backInfo = sceneState.previousScenes.map { SceneInfo(it) },
  )
  val readGestureProgress = remember(gestureState) {
    { (gestureState.transitionState as? NavigationEventTransitionState.InProgress)?.latestEvent?.progress }
  }
  NavigationBackHandler(
    state = gestureState,
    isBackEnabled = scene.previousEntries.isNotEmpty(),
    onBackCompleted = { repeat(entries.size - scene.previousEntries.size) { onBack() } },
  )
  CompositionLocalProvider(
    LocalPredictiveGestureProgress provides readGestureProgress,
  ) {
    NavDisplay(
      sceneState = sceneState,
      navigationEventState = gestureState,
      transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
      popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
      predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
    )
  }
}

private class DeviceCornerSceneStrategy<T : Any>(
  private val delegate: SceneStrategy<T>,
  private val corners: DeviceCorners?,
) : SceneStrategy<T> {
  override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
    val scene = with(delegate) { calculateScene(entries) } ?: return null
    return DeviceCornerScene(scene, corners)
  }
}

private data class DeviceCornerScene<T : Any>(
  val delegate: Scene<T>,
  val corners: DeviceCorners?,
) : Scene<T> by delegate {
  override val content: @Composable () -> Unit = {
    val transition = LocalNavAnimatedContentScope.current.transition
    val readGestureProgress = LocalPredictiveGestureProgress.current
    val gestureProgress = readGestureProgress()
    var predictiveExit by remember { mutableStateOf(false) }
    val cornerVisibility = remember { Animatable(1f) }
    // Keep both pages mounted during entry/return; the list underneath stays untransformed.
    val timelineProgress by transition.animateFloat(
      transitionSpec = {
        tween(PredictiveMotionDuration, easing = LinearEasing)
      },
      label = "page scale progress",
    ) { if (it == EnterExitState.Visible) 0f else 1f }
    SideEffect {
      if (gestureProgress != null && transition.currentState == EnterExitState.Visible &&
        transition.targetState == EnterExitState.PostExit) {
        predictiveExit = true
      }
    }
    val cancelling = predictiveExit && gestureProgress == null &&
      transition.targetState == EnterExitState.Visible
    LaunchedEffect(cancelling, gestureProgress != null) {
      if (gestureProgress != null) {
        cornerVisibility.snapTo(1f)
      } else if (cancelling) {
        // The page may finish restoring before this animation (especially after a short drag).
        // Keep its clip until the visible radius reaches the original unclipped contour.
        cornerVisibility.animateTo(0f, tween(PredictiveMotionDuration))
        predictiveExit = false
      }
    }
    // Apply direct progress once NavDisplay has mounted the previous scene. Otherwise the very
    // first progress event would expose an empty frame before the scene seek effect has run.
    val outgoing = predictiveExit || (gestureProgress != null &&
      transition.currentState == EnterExitState.Visible && transition.targetState == EnterExitState.PostExit)
    val ordinaryMotion = delegate.previousEntries.isNotEmpty() &&
      (transition.currentState != EnterExitState.Visible || transition.targetState != EnterExitState.Visible)
    val transforming = outgoing || ordinaryMotion
    LaunchedEffect(ordinaryMotion, predictiveExit) {
      if (ordinaryMotion && !predictiveExit) cornerVisibility.snapTo(1f)
    }
    fun motionProgress(): Float = if (transforming) {
      SheetBackEasing.transform((readGestureProgress() ?: timelineProgress).coerceIn(0f, 1f))
    } else 0f
    val backdropVisibility = remember { Animatable(1f) }
    LaunchedEffect(outgoing, gestureProgress != null) {
      if (!predictiveExit || (outgoing && gestureProgress != null)) {
        backdropVisibility.snapTo(1f)
      } else if (outgoing) {
        backdropVisibility.animateTo(0f, tween(PredictiveMotionDuration))
      }
    }
    // Untransformed backdrop draws over the previous page and below the scaled current page.
    Box(Modifier.fillMaxSize().drawBehind {
      if (transforming) {
        drawRect(Color.Gray.copy(alpha = 0.24f * motionProgress() * backdropVisibility.value))
      }
    }) {
      Box(Modifier.fillMaxSize().graphicsLayer {
        val fraction = motionProgress()
        val pose = PageMotionGeometry.transform(size, fraction, 48.dp.toPx(), 24.dp.toPx())
        scaleX = pose.scaleX
        scaleY = pose.scaleY
        translationX = pose.translationX
        translationY = pose.translationY
        transformOrigin = TransformOrigin(0f, 0f)
      }) {
        // Material's child correction preserves text/image proportions under anisotropic scaling.
        Box(Modifier.fillMaxSize().graphicsLayer {
          val fraction = motionProgress()
          val pose = PageMotionGeometry.transform(size, fraction, 48.dp.toPx(), 24.dp.toPx())
          scaleY = if (pose.scaleY > 0f) pose.scaleX / pose.scaleY else 1f
          transformOrigin = TransformOrigin(0.5f, 0.5f)
          // This layer is the visible page. Its total X/Y scale is pose.scaleX on both axes.
          clip = transforming
          if (corners != null) {
            shape = DeviceCornerShape(corners, pose.scaleX, pose.scaleX, cornerVisibility.value)
          }
        }.background(if (transforming) MaterialTheme.colorScheme.background else Color.Transparent)) {
          delegate.content()
        }
      }
    }
  }
}
