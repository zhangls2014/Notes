package me.zhangls.entry.panes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.layout.LocalAnimatedPaneOverride
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LookaheadScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.scene.SinglePaneSceneStrategy
import me.zhangls.navigation.SafeAreaSceneStrategy
import me.zhangls.theme.layout.LocalPaneScaffoldDirective
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo

/** Shared partition and sizing inputs for the root list–detail scenes. */
internal class ListDetailPaneController(
  val expandedDirective: PaneScaffoldDirective,
  val hasHinge: Boolean,
  val keepPhysicalSplit: Boolean,
  hasHorizontalHinge: Boolean,
) {
  val canSplitHorizontally = expandedDirective.maxHorizontalPartitions > 1

  // A tall, narrow window alone should still use the whole-page back animation.
  val retainAdaptiveSceneForSinglePane = canSplitHorizontally || hasHorizontalHinge

  // The expanded directive retains Material's preferred widths and hinge bounds.
  val fullWidthListDirective = expandedDirective.copy(
    maxHorizontalPartitions = 1,
    maxVerticalPartitions = 1,
    excludedBounds = emptyList(),
  )
}

@Composable
internal fun rememberListDetailPaneController(): ListDetailPaneController {
  val directive = LocalPaneScaffoldDirective.current
  val hinges = LocalWindowAdaptiveInfo.current.windowPosture.hingeList
  return remember(directive, hinges) {
    ListDetailPaneController(
      expandedDirective = directive,
      hasHinge = hinges.isNotEmpty(),
      keepPhysicalSplit = hinges.any { !it.isFlat },
      hasHorizontalHinge = hinges.any { !it.isVertical },
    )
  }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun <T : Any> rememberListDetailSceneStrategies(
  paneController: ListDetailPaneController,
  detailClosing: Boolean,
  sceneInsets: Map<Set<Any>, WindowInsetsSides>,
): List<SceneStrategy<T>> {
  val adaptiveStrategy = rememberListDetailSceneStrategy<T>(
    shouldHandleSinglePaneLayout = paneController.retainAdaptiveSceneForSinglePane,
    directive = paneController.expandedDirective,
  )
  val fullWidthListStrategy = rememberListDetailSceneStrategy<T>(
    shouldHandleSinglePaneLayout = true,
    directive = paneController.fullWidthListDirective,
  )
  return remember(
    adaptiveStrategy,
    fullWidthListStrategy,
    paneController,
    detailClosing,
    sceneInsets,
  ) {
    // Only root navigation panes reserve scene-wide safe areas. Single pages own their insets.
    val paneStrategy = if (paneController.hasHinge) adaptiveStrategy else SafeAreaSceneStrategy(adaptiveStrategy)
    val onDemandStrategies = sceneInsets.map { (sceneKeys, insetSides) ->
      val strategy = OnDemandDetailSceneStrategy(
        expanded = adaptiveStrategy,
        collapsed = fullWidthListStrategy,
        handleSinglePane = paneController.retainAdaptiveSceneForSinglePane,
        closing = detailClosing,
        keepPhysicalSplit = paneController.keepPhysicalSplit,
        sceneKeys = sceneKeys,
      )
      if (sceneKeys == noticesSceneKeys || !paneController.hasHinge) {
        SafeAreaSceneStrategy(strategy, insetSides.takeUnless { paneController.hasHinge })
      } else {
        strategy
      }
    }

    onDemandStrategies + listOf(paneStrategy, SinglePaneSceneStrategy())
  }
}

private const val OnDemandPaneKey = "on-demand-pane"

internal fun onDemandPaneMetadata(sceneKey: Any, isList: Boolean): Map<String, Any> =
  mapOf(OnDemandPaneKey to OnDemandPane(sceneKey, isList))

private data class OnDemandPane(val sceneKey: Any, val isList: Boolean)

private val NavEntry<*>.onDemandPane: OnDemandPane?
  get() = metadata[OnDemandPaneKey] as? OnDemandPane

/** Keep one native scaffold scene while its list expands and detail animates out. */
private class OnDemandDetailSceneStrategy<T : Any>(
  private val expanded: SceneStrategy<T>,
  private val collapsed: SceneStrategy<T>,
  private val handleSinglePane: Boolean,
  private val closing: Boolean,
  private val keepPhysicalSplit: Boolean,
  private val sceneKeys: Set<Any>,
) : SceneStrategy<T> {
  override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
    if (!handleSinglePane) return null
    val pane = entries.lastOrNull()?.onDemandPane ?: return null
    if (pane.sceneKey !in sceneKeys) return null
    val listOnly = pane.isList
    // The real backstack retains the outgoing detail until motion completes. Putting the list
    // last makes it the native scaffold's current destination without dropping detail content.
    val history = if (closing && !listOnly) {
      val listPane = OnDemandPane(pane.sceneKey, isList = true)
      val listIndex = entries.indexOfLast { it.onDemandPane == listPane }
      check(listIndex >= 0) { "Detail requires a list entry" }
      entries.toMutableList().apply { add(removeAt(listIndex)) }
    } else {
      entries
    }
    val strategy = if ((listOnly || closing) && !keepPhysicalSplit) collapsed else expanded
    return with(strategy) { calculateScene(history) }?.let { scene ->
      OnDemandPaneScene(
        scene,
        entries,
        entries.dropLast(1),
        detailVisible = if (keepPhysicalSplit) !listOnly && !closing else null,
        showPlaceholder = listOnly,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3AdaptiveComponentOverrideApi::class)
private data class OnDemandPaneScene<T : Any>(
  val delegate: Scene<T>,
  override val entries: List<NavEntry<T>>,
  override val previousEntries: List<NavEntry<T>>,
  val detailVisible: Boolean?,
  val showPlaceholder: Boolean,
) : Scene<T> by delegate {
  override val content: @Composable () -> Unit = {
    val defaultPane = LocalAnimatedPaneOverride.current
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
      LookaheadScope {
        val bounds = remember(defaultPane, this, detailVisible, showPlaceholder) {
          ListDetailPaneBounds(defaultPane, this, detailVisible, showPlaceholder)
        }
        CompositionLocalProvider(LocalAnimatedPaneOverride provides bounds) {
          delegate.content()
        }
      }
    }
  }
}
