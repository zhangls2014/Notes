package me.zhangls.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/** Reserve the shared safe area before the delegate lays out its panes. */
class SafeAreaSceneStrategy<T : Any>(
  private val delegate: SceneStrategy<T>,
  private val insetSides: WindowInsetsSides = WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical,
) : SceneStrategy<T> {
  override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
    val scene = with(delegate) { calculateScene(entries) } ?: return null
    return SafeAreaScene(scene, insetSides)
  }
}

private data class SafeAreaScene<T : Any>(val delegate: Scene<T>, val insetSides: WindowInsetsSides) :
  Scene<T> by delegate {
  override val content: @Composable () -> Unit = {
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(insetSides))) {
      delegate.content()
    }
  }
}
