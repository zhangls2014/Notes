package me.zhangls.entry

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import me.zhangls.theme.layout.LocalPaneScaffoldDirective
import me.zhangls.theme.layout.LocalWindowAdaptiveInfo

/** Root scene policy shared by all destinations; page-local pane layouts are independent. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun <T : Any> rememberAppSceneStrategies(): List<SceneStrategy<T>> {
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
  val hasHinge = hinges.isNotEmpty()
  return remember(adaptiveStrategy, hasHinge) {
    // Only root navigation panes reserve scene-wide safe areas. Single pages own their insets.
    val paneStrategy = if (hasHinge) adaptiveStrategy else SafeAreaSceneStrategy(adaptiveStrategy)
    listOf(paneStrategy, SinglePaneSceneStrategy())
  }
}
