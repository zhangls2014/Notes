package me.zhangls.entry

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.layout.LocalAnimatedPaneOverride
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
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
import me.zhangls.about.api.OpenSourceNoticesDestination
import me.zhangls.about.api.OpenSourcePage
import me.zhangls.email.api.EmailDetailDestination
import me.zhangls.email.api.EmailListScene
import me.zhangls.main.EmailDetailPlaceholder
import me.zhangls.navigation.SafeAreaSceneStrategy
import me.zhangls.theme.layout.LocalPaneScaffoldDirective
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo

internal const val PANE_ANIMATION_DURATION_MILLIS = 250
private const val DETAIL_EXIT_DURATION_MILLIS = 200

// Allow lookahead measurement and its animation clock to settle before disposing the outgoing detail.
internal const val PANE_CLOSE_DURATION_MILLIS = 350

/** Root scene policy shared by all destinations; page-local pane layouts are independent. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun <T : Any> rememberAppSceneStrategies(detailClosing: Boolean = false): List<SceneStrategy<T>> {
  val directive = LocalPaneScaffoldDirective.current
  val hinges = LocalWindowAdaptiveInfo.current.windowPosture.hingeList
  // Adaptive also recommends two vertical partitions for tall, narrow windows. That capability
  // alone must not retain its single-pane scene and bypass the app's whole-page back animation.
  // Preserve wide-screen placeholders and physical horizontal-hinge layouts even with one entry.
  val retainAdaptiveSceneForSinglePane = directive.maxHorizontalPartitions > 1 || hinges.any { !it.isVertical }
  val adaptiveStrategy = rememberListDetailSceneStrategy<T>(
    shouldHandleSinglePaneLayout = retainAdaptiveSceneForSinglePane,
    directive = directive,
  )
  val fullWidthListStrategy = rememberListDetailSceneStrategy<T>(
    shouldHandleSinglePaneLayout = true,
    directive = directive.copy(maxHorizontalPartitions = 1, maxVerticalPartitions = 1, excludedBounds = emptyList()),
  )
  val hasHinge = hinges.isNotEmpty()
  val keepPhysicalSplit = hinges.any { !it.isFlat }
  return remember(
    adaptiveStrategy,
    fullWidthListStrategy,
    hasHinge,
    retainAdaptiveSceneForSinglePane,
    detailClosing,
    keepPhysicalSplit,
  ) {
    // Only root navigation panes reserve scene-wide safe areas. Single pages own their insets.
    val paneStrategy = if (hasHinge) adaptiveStrategy else SafeAreaSceneStrategy(adaptiveStrategy)
    val noticesStrategy = OnDemandDetailSceneStrategy(
      expanded = adaptiveStrategy,
      collapsed = fullWidthListStrategy,
      handleSinglePane = retainAdaptiveSceneForSinglePane,
      closing = detailClosing,
      keepPhysicalSplit = keepPhysicalSplit,
    )
    val safeNoticesStrategy = if (hasHinge) {
      noticesStrategy
    } else {
      SafeAreaSceneStrategy(noticesStrategy, WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    }
    val homeStrategy = OnDemandDetailSceneStrategy(
      expanded = adaptiveStrategy,
      collapsed = fullWidthListStrategy,
      handleSinglePane = retainAdaptiveSceneForSinglePane,
      closing = detailClosing,
      keepPhysicalSplit = keepPhysicalSplit,
      notices = false,
    )
    val safeHomeStrategy = if (hasHinge) homeStrategy else SafeAreaSceneStrategy(homeStrategy)
    listOf(safeNoticesStrategy, safeHomeStrategy, paneStrategy, SinglePaneSceneStrategy())
  }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val noticesListMetadata = ListDetailSceneStrategy.listPane(
  sceneKey = "open-source-notices",
  detailPlaceholder = {},
) + ListDetailSceneStrategy.paneAnimation(boundsAnimationSpec = tween(PANE_ANIMATION_DURATION_MILLIS)) +
  mapOf("notices-list" to true, "notices-scene" to true)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun noticesPaneMetadata(destination: OpenSourceNoticesDestination): Map<String, Any> =
  if (destination.page == OpenSourcePage.LIBRARIES && destination.projectUrl == null) {
    noticesListMetadata
  } else {
    ListDetailSceneStrategy.detailPane(sceneKey = "open-source-notices") +
      detailPaneAnimation + mapOf("notices-scene" to true)
  }

/** Keep one native scaffold scene while its list expands and detail animates out. */
private class OnDemandDetailSceneStrategy<T : Any>(
  private val expanded: SceneStrategy<T>,
  private val collapsed: SceneStrategy<T>,
  private val handleSinglePane: Boolean,
  private val closing: Boolean,
  private val notices: Boolean = true,
  private val keepPhysicalSplit: Boolean,
) : SceneStrategy<T> {
  override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
    val last = entries.lastOrNull()
    val marker = if (notices) "notices-scene" else "home-email-scene"
    val matches = last?.metadata?.get(marker) == true
    if (!handleSinglePane || !matches) return null
    val listOnly = entries.last().isListPane()
    // The real backstack retains the outgoing detail until motion completes. Putting the list
    // last makes it the native scaffold's current destination without dropping detail content.
    val history = if (closing && !listOnly) {
      val listIndex = entries.indexOfLast { it.isListPane() }
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

  private fun NavEntry<T>.isListPane(): Boolean = metadata[if (notices) "notices-list" else "home-email-list"] == true
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal val homeEmailListMetadata = ListDetailSceneStrategy.listPane(
  sceneKey = EmailListScene.Home,
  detailPlaceholder = { EmailDetailPlaceholder(modifier = Modifier) },
) + ListDetailSceneStrategy.paneAnimation(boundsAnimationSpec = tween(PANE_ANIMATION_DURATION_MILLIS)) +
  mapOf("home-email-list" to true, "home-email-scene" to true)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun homeEmailPaneMetadata(destination: EmailDetailDestination): Map<String, Any> =
  ListDetailSceneStrategy.detailPane(sceneKey = destination.scene) +
    if (destination.scene == EmailListScene.Home) {
      detailPaneAnimation + mapOf("home-email-scene" to true)
    } else {
      emptyMap()
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val detailPaneAnimation = ListDetailSceneStrategy.paneAnimation(
  enterTransition = slideInHorizontally(tween(PANE_ANIMATION_DURATION_MILLIS)) { it } +
    fadeIn(tween(PANE_ANIMATION_DURATION_MILLIS)),
  exitTransition = slideOutHorizontally(tween(DETAIL_EXIT_DURATION_MILLIS)) { it } +
    fadeOut(tween(DETAIL_EXIT_DURATION_MILLIS)),
  boundsAnimationSpec = tween(PANE_ANIMATION_DURATION_MILLIS),
)

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
