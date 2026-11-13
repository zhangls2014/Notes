package me.zhangls.entry.panes

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.navigation3.scene.SceneStrategy

internal const val PANE_ANIMATION_DURATION_MILLIS = 250
private const val DETAIL_EXIT_DURATION_MILLIS = 200

// Allow lookahead measurement and its animation clock to settle before disposing the outgoing detail.
internal const val PANE_CLOSE_DURATION_MILLIS = 350

@Composable
internal fun <T : Any> rememberAppSceneStrategies(
  detailClosing: Boolean = false,
  paneController: ListDetailPaneController = rememberListDetailPaneController(),
): List<SceneStrategy<T>> = rememberListDetailSceneStrategies(paneController, detailClosing, destinationPaneInsets)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun listPaneMetadata(
  sceneKey: Any,
  detailPlaceholder: @Composable () -> Unit,
): Map<String, Any> =
  ListDetailSceneStrategy.listPane(sceneKey = sceneKey) { detailPlaceholder() } +
    ListDetailSceneStrategy.paneAnimation(boundsAnimationSpec = tween(PANE_ANIMATION_DURATION_MILLIS)) +
    onDemandPaneMetadata(sceneKey, isList = true)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun detailPaneMetadata(sceneKey: Any): Map<String, Any> =
  ListDetailSceneStrategy.detailPane(sceneKey = sceneKey) +
    detailPaneAnimation + onDemandPaneMetadata(sceneKey, isList = false)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val detailPaneAnimation = ListDetailSceneStrategy.paneAnimation(
  enterTransition = slideInHorizontally(tween(PANE_ANIMATION_DURATION_MILLIS)) { it } +
    fadeIn(tween(PANE_ANIMATION_DURATION_MILLIS)),
  exitTransition = slideOutHorizontally(tween(DETAIL_EXIT_DURATION_MILLIS)) { it } +
    fadeOut(tween(DETAIL_EXIT_DURATION_MILLIS)),
  boundsAnimationSpec = tween(PANE_ANIMATION_DURATION_MILLIS),
)
