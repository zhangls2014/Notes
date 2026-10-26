package me.zhangls.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInput
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

const val PredictiveMotionDuration = 220
const val PageExitFadeDuration = 120
private const val PreviewTimelineFraction = PredictiveMotionDuration.toFloat() / (PredictiveMotionDuration + PageExitFadeDuration)
private val SheetBackEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
private val LocalPredictiveGestureProgress = staticCompositionLocalOf<() -> Float?> { { null } }
private data class GestureAnchor(val x: Float, val committed: Boolean = false)
private val LocalGestureAnchors = staticCompositionLocalOf<MutableMap<Any, GestureAnchor>?> { null }
private val LocalActiveEntryKeys = staticCompositionLocalOf<Set<Any>> { emptySet() }
private val LocalPageMotionState = staticCompositionLocalOf<PageMotionState?> { null }

/** Read the same curve as the page layer, without copying progress through an effect. */
class PageMotionState {
  internal val readers = mutableStateMapOf<Any, () -> Float>()
  val hasPages: Boolean get() = readers.isNotEmpty()
  val navigationProgress: Float
    get() = when (readers.size) {
      0 -> 1f

      1 -> readers.values.single().invoke()

      // Moving between two full-screen pages must keep the navigation hidden underneath.
      else -> 0f
    }
}

/** Single-page entry, return and predictive back share the Material bottom sheet scale curve. */
@Composable
fun <T : Any> DeviceCornerNavDisplay(
  entries: List<NavEntry<T>>,
  sceneStrategies: List<SceneStrategy<T>>,
  onBack: () -> Unit,
  backEnabled: Boolean? = null,
  pageMotionState: PageMotionState? = null,
) {
  DeviceCornerNavDisplay(
    entries = entries,
    sceneStrategies = sceneStrategies,
    onBack = onBack,
    corners = rememberDeviceCorners(),
    backEnabled = backEnabled,
    pageMotionState = pageMotionState,
  )
}

@Composable
internal fun <T : Any> DeviceCornerNavDisplay(
  entries: List<NavEntry<T>>,
  sceneStrategies: List<SceneStrategy<T>>,
  onBack: () -> Unit,
  corners: DeviceCorners?,
  backEnabled: Boolean? = null,
  pageMotionState: PageMotionState? = null,
) {
  val resolvedCorners = corners.orDefault(LocalDensity.current)
  val strategies = remember(sceneStrategies, resolvedCorners) {
    sceneStrategies.map { strategy ->
      if (strategy is SinglePaneSceneStrategy<T>) DeviceCornerSceneStrategy(strategy, resolvedCorners)
      else strategy
    }
  }
  val sceneState = rememberSceneState(entries = entries, sceneStrategies = strategies, onBack = onBack)
  val scene = sceneState.currentScene
  // Reserve a commit tail even when the physical gesture has already reached 100%.
  val transitionInput = remember { PageTransitionInput() }
  val readGestureProgress = remember(transitionInput) { { transitionInput.gestureProgress } }
  val transitionGestureState = rememberNavigationEventState(
    currentInfo = SceneInfo(scene),
    backInfo = sceneState.previousScenes.map { SceneInfo(it) },
  )
  CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides transitionInput) {
    NavigationBackHandler(state = transitionGestureState, onBackCompleted = {})
  }
  val onComplete by rememberUpdatedState {
    repeat(entries.size - scene.previousEntries.size) { onBack() }
  }
  val dispatcher = checkNotNull(LocalNavigationEventDispatcherOwner.current).navigationEventDispatcher
  val gestureSceneKey by rememberUpdatedState(if (scene is DeviceCornerScene<*>) scene.key else null)
  val handler = remember(transitionInput) {
    object : NavigationEventHandler<SceneInfo<T>>(SceneInfo(scene), isBackEnabled = false, isForwardEnabled = false) {
      override fun onBackStarted(event: NavigationEvent) = transitionInput.progress(event, gestureSceneKey)
      override fun onBackProgressed(event: NavigationEvent) = transitionInput.progress(event, gestureSceneKey)
      override fun onBackCancelled() = transitionInput.cancel()
      override fun onBackCompleted() {
        transitionInput.complete()
        onComplete()
      }
    }
  }
  SideEffect {
    handler.isBackEnabled = backEnabled ?: scene.previousEntries.isNotEmpty()
    handler.setInfo(SceneInfo(scene), sceneState.previousScenes.map { SceneInfo(it) })
  }
  DisposableEffect(dispatcher, handler) {
    dispatcher.addHandler(handler)
    onDispose { handler.remove() }
  }
  DisposableEffect(transitionInput) {
    onDispose { transitionInput.navigationEventDispatcher.dispose() }
  }
  CompositionLocalProvider(
    LocalPredictiveGestureProgress provides readGestureProgress,
    LocalGestureAnchors provides transitionInput.anchors,
    LocalActiveEntryKeys provides remember(entries) { entries.map { it.contentKey }.toSet() },
    LocalPageMotionState provides pageMotionState,
  ) {
    NavDisplay(
      sceneState = sceneState,
      navigationEventState = transitionGestureState,
      transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
      popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
      predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
    )
  }
}

private class PageTransitionInput : NavigationEventInput(), NavigationEventDispatcherOwner {
  override val navigationEventDispatcher = NavigationEventDispatcher().also { it.addInput(this) }
  private var lastEvent: NavigationEvent? = null
  private var gestureSceneKey: Any? = null
  val anchors = mutableStateMapOf<Any, GestureAnchor>()
  var gestureProgress: Float? by mutableStateOf(null)
    private set

  fun progress(event: NavigationEvent, sceneKey: Any?) {
    if (event == lastEvent) return
    if (lastEvent == null && sceneKey != null) {
      gestureSceneKey = sceneKey
      val anchorX = when (event.swipeEdge) {
        NavigationEvent.EDGE_LEFT -> 0.75f
        NavigationEvent.EDGE_RIGHT -> 0.25f
        else -> 0.5f
      }
      anchors[sceneKey] = GestureAnchor(anchorX)
    }
    gestureProgress = event.progress
    val preview = NavigationEvent(event.swipeEdge, event.progress * PreviewTimelineFraction,
      event.touchX, event.touchY, event.frameTimeMillis)
    if (lastEvent == null) {
      dispatchOnBackStarted(preview)
    }
    lastEvent = event
    dispatchOnBackProgressed(preview)
  }

  fun cancel() {
    if (lastEvent != null) dispatchOnBackCancelled()
    lastEvent = null
    gestureSceneKey = null
    gestureProgress = null
  }

  fun complete() {
    gestureSceneKey?.let { key -> anchors[key]?.let { anchors[key] = it.copy(committed = true) } }
    if (lastEvent != null) dispatchOnBackCompleted()
    lastEvent = null
    gestureSceneKey = null
    gestureProgress = null
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
    val gestureAnchors = LocalGestureAnchors.current
    val activeEntryKeys = LocalActiveEntryKeys.current
    val committed = delegate.previousEntries.isNotEmpty() && entries.none { it.contentKey in activeEntryKeys }
    var predictiveExit by remember { mutableStateOf(false) }
    var lastGestureProgress by remember { mutableFloatStateOf(0f) }
    // Keep both pages mounted during entry/return; the list underneath stays untransformed.
    val timelineProgress by transition.animateFloat(
      transitionSpec = {
        // Root lists move between single-pane and list-detail scenes on resize. They never
        // scale out; retaining their scene for an invisible animation overlaps iOS overlays.
        if (delegate.previousEntries.isEmpty()) snap()
        else tween(if (gestureProgress != null || predictiveExit || committed)
          PredictiveMotionDuration + PageExitFadeDuration else PredictiveMotionDuration, easing = LinearEasing)
      },
      label = "page scale progress",
    ) { if (it == EnterExitState.Visible) 0f else 1f }
    DisposableEffect(gestureAnchors, key) {
      onDispose { gestureAnchors?.remove(key) }
    }
    SideEffect {
      if (gestureProgress != null && transition.currentState == EnterExitState.Visible &&
        transition.targetState == EnterExitState.PostExit) {
        predictiveExit = true
        lastGestureProgress = gestureProgress
      } else if (gestureProgress == null && transition.currentState == EnterExitState.Visible &&
        transition.targetState == EnterExitState.Visible) {
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
    fun commitProgress(): Float {
      return ((timelineProgress - PreviewTimelineFraction) / (1f - PreviewTimelineFraction)).coerceIn(0f, 1f)
    }
    fun pageOpacity(): Float = if (committed) 1f - commitProgress() else 1f
    fun motionProgress(): Float = if (transforming) {
      val progress = readGestureProgress() ?: if (committed || predictiveExit) {
        timelineProgress / PreviewTimelineFraction
      } else timelineProgress
      SheetBackEasing.transform(progress.coerceIn(0f, 1f))
    } else 0f
    val gestureEndedOnPage = gestureProgress == null && !committed
    val restored = gestureEndedOnPage && gestureAnchors?.containsKey(key) == true && motionProgress() == 0f
    SideEffect {
      // Reset from the visible pose, not a retained seek timeline; all anchors coincide at 100%.
      if (restored) gestureAnchors?.remove(key)
    }
    val motionState = LocalPageMotionState.current
    if (motionState != null && delegate.previousEntries.isNotEmpty()) {
      val readMotion by rememberUpdatedState({ motionProgress() })
      DisposableEffect(motionState, key) {
        val reader = { readMotion() }
        motionState.readers[key] = reader
        onDispose {
          if (motionState.readers[key] === reader) motionState.readers.remove(key)
        }
      }
    }
    // Untransformed backdrop draws over the previous page and below the scaled current page.
    Box(Modifier.fillMaxSize().drawBehind {
      if (transforming) {
        drawRect(Color.Gray.copy(alpha = 0.24f * motionProgress() * pageOpacity()))
      }
    }) {
      Box(Modifier.fillMaxSize().graphicsLayer {
        alpha = pageOpacity()
        val fraction = motionProgress()
        val anchor = gestureAnchors?.get(key)?.takeIf { !committed || it.committed }
        val pose = PageMotionGeometry.transform(size, fraction, anchor?.x ?: 0.5f)
        scaleX = pose.scaleX
        scaleY = pose.scaleY
        translationX = pose.translationX
        translationY = pose.translationY
        transformOrigin = TransformOrigin(0f, 0f)
      }) {
        // Clip the visible page, compensating corner radii for its uniform scale.
        Box(Modifier.fillMaxSize().graphicsLayer {
          val fraction = motionProgress()
          val pose = PageMotionGeometry.transform(size, fraction)
          // This layer is the visible page. Its total X/Y scale is pose.scaleX on both axes.
          clip = transforming
          if (corners != null) {
            // Cancellation reverses the same timeline as scale; committing keeps full corners.
            val cornerVisibility = if (predictiveExit && readGestureProgress() == null) {
              val releasedFraction = SheetBackEasing.transform(lastGestureProgress.coerceIn(0f, 1f))
              if (releasedFraction > 0f) (fraction / releasedFraction).coerceIn(0f, 1f) else 0f
            } else 1f
            shape = DeviceCornerShape(corners, pose.scaleX, pose.scaleX, cornerVisibility)
          }
        }.background(if (transforming) MaterialTheme.colorScheme.background else Color.Transparent)) {
          delegate.content()
        }
      }
    }
  }
}
