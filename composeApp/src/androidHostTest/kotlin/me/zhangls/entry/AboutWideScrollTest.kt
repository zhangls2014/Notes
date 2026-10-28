package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import me.zhangls.about.api.AboutAppInfo
import me.zhangls.about.api.AboutEntry
import me.zhangls.theme.ComposeAppTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.koinInject
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w1000dp-h400dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutWideScrollTest {
  @get:Rule val compose = createComposeRule()

  @Before fun start() {
    initKoin(null)
  }

  @After fun stop() {
    stopKoin()
  }

  @Test fun leftMarginScrollsThePage() {
    assertMarginScrolls(32f)
  }

  @Test fun rightMarginScrollsThePage() {
    assertMarginScrolls(968f)
  }

  private fun assertMarginScrolls(x: Float) {
    compose.setContent {
      Box(Modifier.requiredSize(1000.dp, 400.dp).testTag("about-window")) {
        ComposeAppTheme {
          koinInject<AboutEntry>().Screen(AboutAppInfo("1.0", "1"), onBack = {}, onOpenSource = {})
        }
      }
    }
    val range = compose.onNode(hasScrollAction()).fetchSemanticsNode()
      .config[SemanticsProperties.VerticalScrollAxisRange]
    assertTrue("The short wide viewport must have overflow", range.maxValue() > 0f)
    val versionBefore = compose.onNodeWithText("Version").fetchSemanticsNode().boundsInRoot
    compose.onNodeWithTag("about-window").performTouchInput {
      swipe(Offset(x, 360f), Offset(x, 120f), durationMillis = 500)
    }
    compose.runOnIdle { assertTrue("Dragging the blank margin must scroll the page", range.value() > 0f) }
    val versionAfter = compose.onNodeWithText("Version").fetchSemanticsNode().boundsInRoot
    assertEquals("Scrolling preserves horizontal content placement", versionBefore.left, versionAfter.left, 0.5f)
  }
}
