package me.zhangls.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.Posture
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [37], qualifiers = "en-rUS-w1200dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReleaseHistoryPostureTest {
  private var motionScale = 1f
  @get:Rule val compose = createComposeRule(effectContext = object : MotionDurationScale {
    override val scaleFactor: Float get() = motionScale
  })
  private var posture by mutableStateOf(fold(flat = true))
  private var width by mutableStateOf(1200)
  private var direction = LayoutDirection.Ltr
  private var pageScale by mutableStateOf(1f)
  private var pageTranslation by mutableStateOf(0f)

  private fun fold(flat: Boolean, vertical: Boolean = true) = Posture(
    hingeList = listOf(HingeInfo(Rect(360f, 0f, 380f, 1000f), flat, vertical, !flat, false)),
  )

  private fun launch() {
    val entry: AboutEntry = AboutEntryImpl()
    compose.setContent {
      Box(Modifier.requiredSize(width.dp, 1000.dp).testTag("posture-window")) {
        ProvideWindowAdaptiveInfo(WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(width, 1000), posture) {
          ComposeAppTheme {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
              Box(Modifier.fillMaxSize().graphicsLayer {
                scaleX = pageScale
                scaleY = pageScale
                translationX = pageTranslation
                translationY = (1f - pageScale) * 500f
                transformOrigin = TransformOrigin(0f, 0f)
              }) {
                entry.ReleaseHistoryScreen(onBack = {})
              }
            }
          }
        }
      }
    }
    compose.waitUntil(timeoutMillis = 10_000) {
      compose.onAllNodes(hasText("1.1.0")).fetchSemanticsNodes().isNotEmpty()
    }
  }

  @Test fun fullOpenDefaultsToSingleAndManualSplitAvoidsOffCenterHinge() {
    launch()
    assertEquals(1168f, cardWidth(), 1f)
    compose.onNodeWithText("Switch to columns").assertDoesNotExist()
    compose.onNodeWithContentDescription("Switch to columns").performClick()
    val card = compose.onNodeWithTag("release-1.1.0").getUnclippedBoundsInRoot()
    assertTrue("Card must end before the off-center hinge", card.right.value <= 360f)
    assertEquals(336f, cardWidth(), 1f)
    compose.onNodeWithContentDescription("Switch to single column").performClick()
    assertEquals(1168f, cardWidth(), 1f)
  }

  @Test fun halfOpenAndClosedHideButtonAndReturningToFlatResetsSelection() {
    launch()
    compose.onNodeWithContentDescription("Switch to columns").performClick()
    compose.runOnIdle { posture = fold(flat = false) }
    compose.onNodeWithContentDescription("Switch to columns").assertDoesNotExist()
    compose.onNodeWithContentDescription("Switch to single column").assertDoesNotExist()
    assertEquals(576f, cardWidth(), 1f)
    compose.runOnIdle { posture = fold(flat = true) }
    assertEquals(1168f, cardWidth(), 1f)
    compose.onNodeWithContentDescription("Switch to columns").performClick()
    compose.runOnIdle {
      posture = Posture()
      width = 400
    }
    compose.onNodeWithContentDescription("Switch to single column").assertDoesNotExist()
    assertEquals(368f, cardWidth(), 1f)
    compose.runOnIdle {
      width = 1200
      posture = fold(flat = true)
    }
    assertEquals(1168f, cardWidth(), 1f)
  }

  @Test fun flatHorizontalHingeHasNoToggleAndOrdinaryTabletKeepsWidthRule() {
    posture = fold(flat = true, vertical = false)
    launch()
    compose.onNodeWithContentDescription("Switch to columns").assertDoesNotExist()
    assertEquals(1168f, cardWidth(), 1f)
    compose.runOnIdle { posture = Posture() }
    compose.onNodeWithContentDescription("Switch to columns").assertDoesNotExist()
    assertEquals(576f, cardWidth(), 1f)
  }

  @Test fun rtlKeepsThePhysicalHingeInPlace() {
    direction = LayoutDirection.Rtl
    launch()
    compose.onNodeWithContentDescription("Switch to columns").performClick()
    val card = compose.onNodeWithTag("release-1.1.0").getUnclippedBoundsInRoot()
    assertTrue("RTL first card belongs to the right physical pane", card.left.value >= 380f)
    assertEquals("Wide pane must be filled after hinge clearance", 796f, cardWidth(), 1f)
    assertEquals(388f, card.left.value, 1f)
    assertEquals(1184f, card.right.value, 1f)
  }

  @Test
  @Config(qualifiers = "zh-rCN-w1200dp-h1000dp-mdpi")
  fun chineseSwitchLabelsDescribeNextAction() {
    launch()
    compose.onNodeWithText("切换为多列").assertDoesNotExist()
    compose.onNodeWithContentDescription("切换为多列").performClick()
    compose.onNodeWithContentDescription("切换为单列").performClick()
    compose.onNodeWithContentDescription("切换为多列").assertExists()
  }

  @Test fun iconRotationHasIntermediateFramesAndSettlesInBothDirections() {
    launch()
    compose.mainClock.autoAdvance = false
    val initial = iconWidth("Switch to columns")
    for ((action, next) in listOf(
      "Switch to columns" to "Switch to single column",
      "Switch to single column" to "Switch to columns",
    )) {
      compose.onNodeWithContentDescription(action).performClick()
      compose.mainClock.advanceTimeByFrame()
      compose.mainClock.advanceTimeBy(48)
      assertTrue("Icon must rotate during the transition", iconWidth(next) > initial + 2f)
      compose.mainClock.advanceTimeBy(240)
      assertEquals(initial, iconWidth(next), 0.5f)
    }
  }

  @Test fun rapidReverseClickFinishesAtLatestLayout() {
    launch()
    compose.mainClock.autoAdvance = false
    val initial = iconWidth("Switch to columns")
    compose.onNodeWithContentDescription("Switch to columns").performClick()
    compose.mainClock.advanceTimeByFrame()
    compose.mainClock.advanceTimeBy(48)
    assertTrue(iconWidth("Switch to single column") > initial + 2f)
    compose.onNodeWithContentDescription("Switch to single column").performClick()
    compose.mainClock.advanceTimeBy(240)
    assertEquals(initial, iconWidth("Switch to columns"), 0.5f)
    assertEquals(1168f, cardWidth(), 1f)
  }

  @Test fun disabledMotionSwitchesImmediatelyWithoutIntermediateRotation() {
    motionScale = 0f
    launch()
    compose.mainClock.autoAdvance = false
    val initial = iconWidth("Switch to columns")
    compose.onNodeWithContentDescription("Switch to columns").performClick()
    compose.mainClock.advanceTimeByFrame()
    compose.mainClock.advanceTimeByFrame()
    assertEquals(initial, iconWidth("Switch to single column"), 0.5f)
    assertEquals(336f, cardWidth(), 1f)
  }

  @Test fun pageTransformPreservesColumnWidthAndWrappingThroughCancellation() {
    assertStableContentDuringPageTransform()
  }

  @Test fun rtlPageTransformPreservesColumnWidthAndWrappingThroughCancellation() {
    direction = LayoutDirection.Rtl
    assertStableContentDuringPageTransform()
  }

  private fun assertStableContentDuringPageTransform() {
    launch()
    compose.onNodeWithContentDescription("Switch to columns").performClick()
    val initial = compose.onNodeWithTag("release-1.1.0").fetchSemanticsNode().layoutInfo.coordinates.size
    for ((scale, translation) in listOf(0.95f to 30f, 0.85f to 90f, 0.9f to 12f, 1f to 0f)) {
      compose.runOnIdle {
        pageScale = scale
        pageTranslation = translation
      }
      val node = compose.onNodeWithTag("release-1.1.0").fetchSemanticsNode()
      assertEquals("Drawing transforms must preserve card width and wrapped height", initial, node.layoutInfo.coordinates.size)
      assertEquals("The visible card must still scale", initial.width * scale, node.boundsInRoot.width, 1f)
    }
  }

  private fun iconWidth(description: String): Float =
    compose.onNodeWithContentDescription(description, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.width

  private fun cardWidth(): Float {
    val bounds = compose.onNodeWithTag("release-1.1.0").getUnclippedBoundsInRoot()
    return bounds.right.value - bounds.left.value
  }
}
