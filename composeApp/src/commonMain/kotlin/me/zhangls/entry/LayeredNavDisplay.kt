package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import me.zhangls.main.AppShell
import me.zhangls.main.api.TabDestination
import me.zhangls.navigation.DeviceCornerNavDisplay
import me.zhangls.navigation.PageMotionState

/** Full-screen pages retain their viewport while the shell underneath animates its navigation. */
@Composable
internal fun LayeredNavDisplay(
  entries: ImmutableList<NavEntry<NavKey>>,
  fullscreenEntryCount: Int,
  displayKey: NavKey,
  contentAlpha: () -> Float,
  selected: TabDestination?,
  onSelectTab: (TabDestination) -> Unit,
  sceneStrategies: ImmutableList<SceneStrategy<NavKey>>,
  onBack: () -> Unit,
) {
  val baseEntries = entries.dropLast(fullscreenEntryCount).toImmutableList()
  val fullscreenEntries = entries.takeLast(fullscreenEntryCount)
  val motionState = remember { PageMotionState() }
  val readNavigationProgress = remember(motionState) { { motionState.navigationProgress } }
  val covered = fullscreenEntries.isNotEmpty() || motionState.hasPages
  val owner = rememberLifecycleOwner(maxLifecycle = if (covered) Lifecycle.State.STARTED else Lifecycle.State.RESUMED)
  // A transparent root lets NavDisplay retain outgoing pages through their existing exit motion.
  // It is only a rendering anchor; the application still owns a single back stack per Tab.
  val overlays = remember(fullscreenEntries) {
    listOf(NavEntry<NavKey>(FullscreenBackdrop) {}) + fullscreenEntries
  }

  Box(Modifier.fillMaxSize()) {
    Box(
      Modifier.focusProperties { canFocus = !covered }
        .then(if (covered) Modifier.clearAndSetSemantics {} else Modifier),
    ) {
      CompositionLocalProvider(LocalLifecycleOwner provides owner) {
        AppShell(selected, onSelectTab, navigationProgress = readNavigationProgress) {
          Box(Modifier.graphicsLayer { alpha = contentAlpha() }) {
            key(displayKey) {
              DeviceCornerNavDisplay(
                entries = baseEntries,
                sceneStrategies = sceneStrategies,
                onBack = onBack,
                backEnabled = if (covered) false else null,
              )
            }
          }
        }
      }
    }
    // The hit target covers the whole viewport, including exposed margins of a shrinking page.
    // Children receive events first; remaining events cannot fall through to the shell.
    val inputShield = if (covered) {
      Modifier.pointerInput(Unit) {
        awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } }
      }
    } else {
      Modifier
    }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha() }.then(inputShield)) {
      key(displayKey) {
        DeviceCornerNavDisplay(
          entries = overlays.toImmutableList(),
          sceneStrategies = remember { persistentListOf(SinglePaneSceneStrategy<NavKey>()) },
          onBack = { if (fullscreenEntries.isNotEmpty()) onBack() },
          // Consume repeated back presses until the outgoing page has finished fading.
          backEnabled = covered,
          pageMotionState = motionState,
        )
      }
    }
  }
}

private data object FullscreenBackdrop : NavKey
