package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/** Reserve the shared safe area before the delegate lays out its panes. */
internal class SafeAreaSceneStrategy<T : Any>(private val delegate: SceneStrategy<T>) : SceneStrategy<T> {
  override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
    val scene = with(delegate) { calculateScene(entries) } ?: return null
    return SafeAreaScene(scene)
  }
}

private data class SafeAreaScene<T : Any>(val delegate: Scene<T>) : Scene<T> by delegate {
  override val content: @Composable () -> Unit = {
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
      delegate.content()
    }
  }
}
