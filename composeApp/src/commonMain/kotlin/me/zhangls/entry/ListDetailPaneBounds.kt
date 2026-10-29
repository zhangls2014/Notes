package me.zhangls.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.layout.AnimatedPaneOverride
import androidx.compose.material3.adaptive.layout.AnimatedPaneOverrideScope
import androidx.compose.material3.adaptive.layout.AnimatedPaneScope
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics

/** The always-visible list uses its own clock for both directions of resizing. */
@OptIn(
  ExperimentalMaterial3AdaptiveApi::class,
  ExperimentalMaterial3AdaptiveComponentOverrideApi::class,
)
internal class ListDetailPaneBounds(
  private val delegate: AnimatedPaneOverride,
  private val lookaheadScope: LookaheadScope,
  private val detailVisible: Boolean? = null,
  private val showPlaceholder: Boolean = false,
) : AnimatedPaneOverride {
  @Composable
  override fun <Role : PaneScaffoldRole, ScaffoldValue : PaneScaffoldValue<Role>> AnimatedPaneOverrideScope<
    Role,
    ScaffoldValue,
    >.AnimatedPane() {
    val pane = this
    if (scope.paneRole == ListDetailPaneScaffoldRole.List) {
      Box(
        modifier.animateBounds(
          lookaheadScope = lookaheadScope,
          boundsTransform = { _, _ -> tween(PANE_ANIMATION_DURATION_MILLIS) },
        ).focusRequester(scope.focusRequesters.getValue(scope.paneRole))
          .focusGroup().semantics { isTraversalGroup = true },
      ) {
        AnimatedVisibility(visible = true) {
          scope.saveableStateHolder.SaveableStateProvider(scope.paneRole.toString()) {
            pane.content(AnimatedPaneScope.create(this@AnimatedVisibility))
          }
        }
      }
    } else if (scope.paneRole == ListDetailPaneScaffoldRole.Detail && detailVisible != null) {
      // Keep a measurable for the empty detail role so the scaffold retains both partitions.
      Box(modifier) {
        if (showPlaceholder) {
          with(delegate) { pane.AnimatedPane() }
        }
        AnimatedVisibility(
          visible = detailVisible,
          enter = enterTransition,
          exit = exitTransition,
        ) {
          with(delegate) { pane.AnimatedPane() }
        }
      }
    } else {
      with(delegate) { pane.AnimatedPane() }
    }
  }
}
