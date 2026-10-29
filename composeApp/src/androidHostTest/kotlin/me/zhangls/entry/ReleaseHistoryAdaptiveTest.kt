package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import me.zhangls.about.AboutEntryImpl
import me.zhangls.about.api.AboutEntry
import me.zhangls.theme.ComposeAppTheme
import me.zhangls.theme.layout.ProvideWindowAdaptiveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w1800dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReleaseHistoryAdaptiveTest(private val width: Int, private val columns: Int, private val fontScale: Float) {
  companion object {
    @JvmStatic
    @ParameterizedRobolectricTestRunner.Parameters(name = "width={0}, columns={1}, fontScale={2}")
    fun scenarios() = listOf(
      arrayOf<Any>(400, 1, 1f),
      arrayOf<Any>(839, 1, 1f),
      arrayOf<Any>(840, 2, 1f),
      arrayOf<Any>(1200, 2, 1f),
      arrayOf<Any>(1599, 2, 1f),
      arrayOf<Any>(1600, 3, 1f),
      arrayOf<Any>(840, 2, 2f),
    )
  }

  @get:Rule val compose = createComposeRule()

  @Test fun columnWidthTracksWindowAndReflowsWithoutLosingCurrentVersion() {
    var windowWidth by mutableStateOf(width)
    val entry: AboutEntry = AboutEntryImpl()
    compose.setContent {
      Box(Modifier.requiredSize(windowWidth.dp, 1000.dp).testTag("release-window")) {
        ProvideWindowAdaptiveInfo(
          WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(windowWidth, 1000),
          Posture(),
        ) {
          ComposeAppTheme(darkTheme = fontScale > 1f, fontScale = fontScale) { entry.ReleaseHistoryScreen(onBack = {}) }
        }
      }
    }
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodes(androidx.compose.ui.test.hasText("1.1.0")).fetchSemanticsNodes().isNotEmpty()
    }
    assertCardWidth(width, columns)
    compose.runOnIdle { windowWidth = 400 }
    assertCardWidth(400, 1)
    compose.runOnIdle { windowWidth = 1600 }
    assertCardWidth(1600, 3)
    compose.onNodeWithText("1.1.0").assertIsDisplayed()
    compose.onNodeWithText("Release notes").assertIsDisplayed()
  }

  private fun assertCardWidth(windowWidth: Int, columnCount: Int) {
    val grid = compose.onNodeWithTag("release-history-grid").getUnclippedBoundsInRoot()
    val card = compose.onNodeWithTag("release-1.1.0").getUnclippedBoundsInRoot()
    val gridWidth = grid.right.value - grid.left.value
    val cardWidth = card.right.value - card.left.value
    assertEquals(windowWidth.toFloat(), gridWidth, 1f)
    assertEquals((gridWidth - 32f - 16f * (columnCount - 1)) / columnCount, cardWidth, 1f)
    assertTrue(card.left >= grid.left && card.right <= grid.right)
  }
}
