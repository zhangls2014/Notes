package me.zhangls.notes.ui.home

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.window.layout.FoldingFeature
import com.google.accompanist.adaptive.calculateDisplayFeatures

/**
 * @author zhangls
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun HomeScreen(viewmodel: HomeViewModel = hiltViewModel(), onHomeResult: (HomeResult) -> Unit) {
  val activity = LocalActivity.current ?: return
  calculateWindowSizeClass(activity)
  val displayFeatures = calculateDisplayFeatures(activity)

  /**
   * We are using display's folding features to map the device postures a fold is in.
   * In the state of folding device If it's half fold in BookPosture we want to avoid content
   * at the crease/hinge
   */
  displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
}

@Preview
@Composable
private fun HomeScreenPreview() {
  HomeScreen(onHomeResult = {})
}